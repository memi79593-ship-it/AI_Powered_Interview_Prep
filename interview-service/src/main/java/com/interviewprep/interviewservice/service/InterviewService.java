package com.interviewprep.interviewservice.service;

import com.interviewprep.interviewservice.dto.AnswerDTO;
import com.interviewprep.interviewservice.entity.Answer;
import com.interviewprep.interviewservice.entity.InterviewSession;
import com.interviewprep.interviewservice.entity.Question;
import com.interviewprep.interviewservice.repository.AnswerRepository;
import com.interviewprep.interviewservice.repository.InterviewSessionRepository;
import com.interviewprep.interviewservice.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core Interview Flow Engine.
 *
 * Manages the complete interview lifecycle:
 * startInterview() → saveQuestions() → submitAnswer() → completeInterview()
 */
@Service
@Transactional
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewSessionRepository sessionRepo;
    private final QuestionRepository questionRepo;
    private final AnswerRepository answerRepo;
    private final AIClientService aiClientService;
    private final AdaptiveDifficultyService adaptiveService;
    private final WeakTopicService weakTopicService;
    private final SkillProfileService skillProfileService;

    public InterviewService(InterviewSessionRepository sessionRepo,
            QuestionRepository questionRepo,
            AnswerRepository answerRepo,
            AIClientService aiClientService,
            AdaptiveDifficultyService adaptiveService,
            WeakTopicService weakTopicService,
            SkillProfileService skillProfileService) {
        this.sessionRepo = sessionRepo;
        this.questionRepo = questionRepo;
        this.answerRepo = answerRepo;
        this.aiClientService = aiClientService;
        this.adaptiveService = adaptiveService;
        this.weakTopicService = weakTopicService;
        this.skillProfileService = skillProfileService;
    }

    // ══════════════════════════════════════════════════════════════
    // DAY 6 – SESSION START
    // ══════════════════════════════════════════════════════════════

    /**
     * Creates a new interview session in STARTED state.
     * Immediately generates and stores questions via Flask/Ollama.
     *
     * @param request Partial session with userEmail, role, type, difficulty
     * @return Saved session (with generated ID)
     */
    public InterviewSession startInterview(InterviewSession request) {
        log.info("Starting interview | user={}, role={}, type={}, difficulty={}",
                request.getUserEmail(), request.getRole(),
                request.getType(), request.getDifficulty());

        // Day 9: Adaptive difficulty – recommend if not supplied by caller
        if (request.getDifficulty() == null || request.getDifficulty().isBlank()) {
            String recommended = adaptiveService.recommendDifficulty(
                    request.getUserEmail(), request.getRole());
            request.setDifficulty(recommended);
            log.info("Adaptive difficulty applied: {}", recommended);
        }

        // Day 9: Detect weak topic from most recent completed session
        String weakTopic = detectWeakTopicForUser(request.getUserEmail(), request.getRole());

        // 1. Create and persist session
        request.setStatus("STARTED");
        request.setCreatedAt(LocalDateTime.now());
        request.setScore(0);
        InterviewSession session = sessionRepo.save(request);

        // 2. Generate and store questions from Flask/Ollama
        try {
            String type = session.getType().toLowerCase();
            log.info("Processing interview type: {}", type);
            
            int count = switch (type) {
                case "subjective" -> {
                    log.info("Generating subjective questions only");
                    yield generateAndSaveSubjective(session, weakTopic);
                }
                case "mcq" -> {
                    log.info("Generating MCQ questions only");
                    yield generateAndSaveMCQ(session, weakTopic);
                }
                case "full" -> {
                    log.info("Generating full mock interview (subjective + MCQ)");
                    // For full interviews, generate both subjective and MCQ questions
                    int subjCount = generateAndSaveSubjective(session, weakTopic);
                    int mcqCount = generateAndSaveMCQ(session, weakTopic);
                    yield subjCount + mcqCount;
                }
                default -> {
                    log.warn("Unknown interview type: {}, defaulting to subjective", type);
                    yield generateAndSaveSubjective(session, weakTopic);
                }
            };

            session.setTotalQuestions(count);
            session.setStatus("IN_PROGRESS");
            sessionRepo.save(session);
            log.info("Session {} started | questions={} | weakTopic={}",
                    session.getId(), count, weakTopic);

        } catch (Exception e) {
            log.error("Failed to generate questions for session {}: {}",
                    session.getId(), e.getMessage());
            
            // Fallback: create basic questions if AI service fails
            try {
                int fallbackCount = createFallbackQuestions(session);
                session.setTotalQuestions(fallbackCount);
                session.setStatus("IN_PROGRESS");
                sessionRepo.save(session);
                log.info("Session {} started with {} fallback questions", session.getId(), fallbackCount);
            } catch (Exception fallbackException) {
                log.error("Failed to create fallback questions for session {}", session.getId(), fallbackException);
            }
        }

        return session;
    }

    /**
     * Day 9: Find primary weak topic from user's most recent completed session for
     * this role.
     */
    private String detectWeakTopicForUser(String email, String role) {
        return sessionRepo.findByUserEmailAndRole(email, role).stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .max(java.util.Comparator.comparing(
                        s -> s.getCreatedAt() != null ? s.getCreatedAt() : java.time.LocalDateTime.MIN))
                .map(s -> weakTopicService.getPrimaryWeakTopic(s.getId()))
                .orElse(null);
    }

    // ── Question Generators ───────────────────────────────────────

    private int generateAndSaveSubjective(InterviewSession session) {
        return generateAndSaveSubjective(session, null);
    }

    private int generateAndSaveSubjective(InterviewSession session, String weakTopic) {
        String rawJson = aiClientService.generateSubjective(
                session.getRole(), session.getDifficulty(), session.getQuestionCount(), weakTopic);
        return parseAndSaveQuestions(rawJson, session.getId(), "subjective");
    }

    private int generateAndSaveMCQ(InterviewSession session) {
        return generateAndSaveMCQ(session, null);
    }

    private int generateAndSaveMCQ(InterviewSession session, String weakTopic) {
        String rawJson = aiClientService.generateMCQ(
                session.getRole(), session.getDifficulty(), session.getQuestionCount(), weakTopic);
        return parseAndSaveMCQQuestions(rawJson, session.getId());
    }

    // Coding question generation removed: coding functionality no longer supported

    // Full mock interview generation removed: coding functionality no longer supported

    /**
     * Parser: extracts questions from Ollama's raw JSON string response.
     * Stores each question as a Question entity. Order is preserved.
     * For subjective interviews, filters out coding-style questions.
     */
    private int parseAndSaveQuestions(String rawJson, Long sessionId, String type) {
        log.info("Parsing {} questions for session {} with type: {}", type, sessionId, type);
        log.debug("Raw JSON response: {}", rawJson);
        
        int count = 0;
        try {
            // Properly parse the JSON array to handle multiple fields per question
            // The JSON format is: [{"question": "...", "topic": "...", "modelAnswer": "..."}, ...]
            
            // Find all question objects in the JSON array
            String cleanedJson = rawJson.replaceAll("^.*?\\[", "[").replaceAll("\\].*$", "]");
            
            // Parse each question object
            int objStart = cleanedJson.indexOf('{');
            while (objStart != -1) {
                int braceCount = 1;
                int objEnd = objStart + 1;
                
                // Find the matching closing brace
                while (objEnd < cleanedJson.length() && braceCount > 0) {
                    char c = cleanedJson.charAt(objEnd);
                    if (c == '{') {
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                    }
                    objEnd++;
                }
                
                if (braceCount == 0) {
                    String questionObj = cleanedJson.substring(objStart, objEnd);
                    
                    // Extract question text
                    String questionText = extractJsonValue(questionObj, "question");
                    if (questionText != null) {
                        // Extract topic
                        String topic = extractJsonValue(questionObj, "topic");
                        if (topic == null) {
                            topic = "General"; // default topic
                        }
                        
                        // Extract model answer
                        String modelAnswer = extractJsonValue(questionObj, "modelAnswer");
                        
                        // If no model answer found, create a placeholder for learning purposes
                        if (modelAnswer == null || modelAnswer.trim().isEmpty()) {
                            modelAnswer = "Model answer not available. This would typically contain a comprehensive response covering key concepts, best practices, and examples relevant to this question.";
                        }
                        
                        Question q = Question.builder()
                                .sessionId(sessionId)
                                .questionText(questionText)
                                .type(type)  // Explicitly set the type
                                .topic(topic)  // Set the topic
                                .correctAnswer(modelAnswer) // Store model answer as correctAnswer for learning purposes
                                .questionOrder(count + 1)
                                .build();
                        questionRepo.save(q);
                        log.debug("Saved {} question {}: {} | Topic: {}", type, count + 1, 
                            questionText.substring(0, Math.min(50, questionText.length())) + "...", topic);
                        count++;
                    }
                }
                
                // Find next object
                objStart = cleanedJson.indexOf('{', objEnd);
            }
        } catch (Exception e) {
            log.error("Failed to parse questions JSON, falling back to legacy parsing: {}", e.getMessage());
            // Fall back to the old parsing method if the new one fails
            count = parseAndSaveQuestionsLegacy(rawJson, sessionId, type);
        }
        
        log.info("Successfully parsed and saved {} {} questions", count, type);
        return count;
    }
    
    /**
     * Legacy parsing method for backward compatibility
     */
    private int parseAndSaveQuestionsLegacy(String rawJson, Long sessionId, String type) {
        log.info("Using legacy parsing for {} questions", type);
        
        // Split by "question": entries to extract individual text blocks
        String[] parts = rawJson.split("\"question\"\\s*:\\s*\"");
        int count = 0;
        for (int i = 1; i < parts.length; i++) {
            String text = parts[i];
            int end = text.indexOf("\"");
            if (end > 0) {
                String questionText = text.substring(0, end).replace("\\n", "\n").trim();
                
                // Allow coding-style questions in subjective interviews for AI evaluation
                // These will be evaluated based on the explanation rather than code execution
                
                // Attempt to extract model answer/expected answer from the JSON if present
                String modelAnswer = extractModelAnswer(text, questionText);
                
                // If no model answer found, create a placeholder for learning purposes
                if (modelAnswer == null || modelAnswer.trim().isEmpty()) {
                    modelAnswer = "Model answer not available. This would typically contain a comprehensive response covering key concepts, best practices, and examples relevant to this question.";
                }
                
                Question q = Question.builder()
                        .sessionId(sessionId)
                        .questionText(questionText)
                        .type(type)  // Explicitly set the type
                        .correctAnswer(modelAnswer) // Store model answer as correctAnswer for learning purposes
                        .questionOrder(i)
                        .build();
                questionRepo.save(q);
                log.debug("Saved {} question {}: {} (including coding questions)", type, i, questionText.substring(0, Math.min(50, questionText.length())) + "...");
                count++;
            }
        }
        return count;
    }
    
    /**
     * Helper method to extract a value from JSON by key
     */
    private String extractJsonValue(String json, String key) {
        // Create the pattern string using string concatenation to avoid escape issues
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        sb.append(key);
        sb.append("\"\\s*:\\s*\"([^\"]*)\"");
        String pattern = sb.toString();
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).replace("\\\\n", "\\n").replace("\\n", "\n").trim();
        }
        return null;
    }
    
    /**
     * Determines if a question is a coding question based on keywords.
     */
    private boolean isCodingQuestion(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return false;
        }
        
        String lowerQuestion = questionText.toLowerCase();
        
        // Check for common coding question indicators
        String[] codingIndicators = {
            "write a function", "implement", "code", "program", "algorithm",
            "function to", "method to", "class to", "create a", "develop",
            "given an array", "given a string", "reverse a", "sort", "find the",
            "calculate", "compute", "design a", "solution", "return", "print",
            "input:", "output:", "sample input", "sample output", "constraints:",
            "write code", "programming", "write a program", "function that"
        };
        
        for (String indicator : codingIndicators) {
            if (lowerQuestion.contains(indicator)) {
                return true;
            }
        }
        
        return false;
    }
        
    private int parseAndSaveMCQQuestions(String rawJson, Long sessionId) {
        log.info("Parsing MCQ questions for session {}", sessionId);
        log.info("Raw MCQ JSON response: {}", rawJson);
            
        // For MCQ we also need to capture correctAnswer, options, topic, and explanation
        String[] parts = rawJson.split("\"question\"\\s*:\\s*\"");
        int count = 0;
        for (int i = 1; i < parts.length; i++) {
            String block = parts[i];
            int qEnd = block.indexOf("\"");
            if (qEnd <= 0)
                continue;
    
            String questionText = block.substring(0, qEnd).replace("\\n", "\n").trim();
    
            // Extract options array first
            String options = "[]";
            int optIdx = block.indexOf("\"options\"");
            if (optIdx >= 0) {
                int optStart = block.indexOf("[", optIdx);
                if (optStart >= 0) {
                    int bracketCount = 1;
                    int optEnd = optStart + 1;
                    while (optEnd < block.length() && bracketCount > 0) {
                        if (block.charAt(optEnd) == '[') bracketCount++;
                        if (block.charAt(optEnd) == ']') bracketCount--;
                        optEnd++;
                    }
                    if (bracketCount == 0) {
                        options = block.substring(optStart, optEnd);
                    }
                }
            }
    
            // Extract correctAnswer
            String correctAnswer = "";
            int caIdx = block.indexOf("\"correctAnswer\"");
            if (caIdx >= 0) {
                int caStart = block.indexOf("\"", caIdx + 15) + 1;
                int caEnd = block.indexOf("\"", caStart);
                if (caStart > 0 && caEnd > caStart) {
                    correctAnswer = block.substring(caStart, caEnd).trim();
                }
            }
    
            // Extract topic
            String topic = "General";
            int topicIdx = block.indexOf("\"topic\"");
            if (topicIdx >= 0) {
                int topicStart = block.indexOf("\"", topicIdx + 7) + 1;
                int topicEnd = block.indexOf("\"", topicStart);
                if (topicStart > 0 && topicEnd > topicStart) {
                    topic = block.substring(topicStart, topicEnd).trim();
                }
            }
    
            // Extract explanation (optional)
            String explanation = null;
            int expIdx = block.indexOf("\"explanation\"");
            if (expIdx >= 0) {
                int expStart = block.indexOf("\"", expIdx + 13) + 1;
                int expEnd = block.indexOf("\"", expStart);
                if (expStart > 0 && expEnd > expStart) {
                    explanation = block.substring(expStart, expEnd).replace("\\n", "\n").trim();
                }
            }

            // Fallback: if correctAnswer is missing, try to infer it from options
            if (correctAnswer.isEmpty() && !options.equals("[]")) {
                try {
                    // Parse options to find the first option that seems correct
                    // This is a basic fallback - in production, you'd want better logic
                    if (options.contains("A)")) {
                        correctAnswer = "A";
                    } else if (options.contains("B)")) {
                        correctAnswer = "B";
                    } else if (options.contains("C)")) {
                        correctAnswer = "C";
                    } else if (options.contains("D)")) {
                        correctAnswer = "D";
                    }
                    log.warn("Missing correctAnswer for question '{}', using fallback: {}", 
                            questionText.substring(0, Math.min(30, questionText.length())) + "...", correctAnswer);
                } catch (Exception e) {
                    log.error("Failed to infer correctAnswer for question: {}", questionText);
                }
            }
    
            Question q = Question.builder()
                    .sessionId(sessionId)
                    .questionText(questionText)
                    .type("mcq")  // Explicitly set type to mcq
                    .correctAnswer(correctAnswer)
                    .options(options)
                    .explanation(explanation)
                    .topic(topic)
                    .questionOrder(i)
                    .build();
            questionRepo.save(q);
            log.info("Saved MCQ question {}: {} | Correct: {} | Options: {} | Topic: {} | Explanation: {}", 
                    i, questionText.substring(0, Math.min(50, questionText.length())) + "...", 
                    correctAnswer, options, topic, explanation != null ? explanation.substring(0, Math.min(60, explanation.length())) + "..." : "N/A");
            count++;
        }
        log.info("Successfully parsed and saved {} MCQ questions", count);
        return count;
    }
    
    /**
     * Extract model answer/expected answer from the JSON response for subjective questions.
     * This helps provide learning material in the session review.
     */
    private String extractModelAnswer(String block, String questionText) {
        try {
            // Look for common patterns that might contain model answers
            // The AI service might return fields like "modelAnswer", "expectedAnswer", 
            // "sampleAnswer", "answer", etc.
            
            // Pattern 1: Look for "answer" field
            int answerIdx = block.indexOf("\"answer\"");
            if (answerIdx >= 0) {
                int start = block.indexOf("\"", answerIdx + 8) + 1;
                int end = block.indexOf("\"", start);
                if (start > 0 && end > start) {
                    String answer = block.substring(start, end);
                    if (!answer.trim().isEmpty()) {
                        return answer.trim();
                    }
                }
            }
            
            // Pattern 2: Look for "modelAnswer" field
            int modelAnswerIdx = block.indexOf("\"modelAnswer\"");
            if (modelAnswerIdx >= 0) {
                int start = block.indexOf("\"", modelAnswerIdx + 13) + 1;
                int end = block.indexOf("\"", start);
                if (start > 0 && end > start) {
                    String answer = block.substring(start, end);
                    if (!answer.trim().isEmpty()) {
                        return answer.trim();
                    }
                }
            }
            
            // Pattern 3: Look for "expectedAnswer" field
            int expectedAnswerIdx = block.indexOf("\"expectedAnswer\"");
            if (expectedAnswerIdx >= 0) {
                int start = block.indexOf("\"", expectedAnswerIdx + 15) + 1;
                int end = block.indexOf("\"", start);
                if (start > 0 && end > start) {
                    String answer = block.substring(start, end);
                    if (!answer.trim().isEmpty()) {
                        return answer.trim();
                    }
                }
            }
            
            // Pattern 4: Look for "sampleAnswer" field
            int sampleAnswerIdx = block.indexOf("\"sampleAnswer\"");
            if (sampleAnswerIdx >= 0) {
                int start = block.indexOf("\"", sampleAnswerIdx + 13) + 1;
                int end = block.indexOf("\"", start);
                if (start > 0 && end > start) {
                    String answer = block.substring(start, end);
                    if (!answer.trim().isEmpty()) {
                        return answer.trim();
                    }
                }
            }
            
            // If no specific answer field found, return null
            return null;
            
        } catch (Exception e) {
            log.warn("Failed to extract model answer for question: {}", questionText.substring(0, Math.min(50, questionText.length())));
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DAY 6 – SUBMIT ANSWER
    // ══════════════════════════════════════════════════════════════

    /**
     * Receives a user's answer, evaluates it, and stores the result.
     * MCQ: auto-scored (0 or 1)
     * Subjective: sent to Ollama for AI scoring (0–10)
     * Coding: stored for manual/output evaluation
     *
     * @return The persisted Answer with score
     */
    public Answer submitAnswer(AnswerDTO dto) {
        log.info("submitAnswer | session={}, question={}", dto.getSessionId(), dto.getQuestionId());

        Question question = questionRepo.findById(dto.getQuestionId())
                .orElseThrow(() -> new RuntimeException(
                        "Question not found: " + dto.getQuestionId()));

        // Save answer without immediate evaluation - evaluation will happen at session completion
        int score = 0;  // Will be set during batch evaluation
        String aiFeedback = null;  // Will be set during batch evaluation

        // Upsert: update existing answer if already submitted
        Answer answer = answerRepo
                .findAnswerBySessionIdAndQuestionId(dto.getSessionId(), dto.getQuestionId())
                .orElse(Answer.builder()
                        .sessionId(dto.getSessionId())
                        .questionId(dto.getQuestionId())
                        .build());

        answer.setUserAnswer(dto.getAnswer());
        answer.setScore(score);
        answer.setAiFeedback(aiFeedback);

        return answerRepo.save(answer);
    }

    // ══════════════════════════════════════════════════════════════
    // DAY 6 – COMPLETE SESSION
    // ══════════════════════════════════════════════════════════════

    /**
     * Finalises an interview session:
     * - Aggregates all answer scores
     * - Sets session status to COMPLETED
     * - Stores completedAt timestamp
     *
     * @return Completed session with final score
     */
    public InterviewSession completeInterview(Long sessionId) {
        log.info("Completing session {}", sessionId);

        InterviewSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        // First, batch evaluate all answers
        batchEvaluateAnswers(sessionId);

        // Then calculate total score
        List<Answer> answers = answerRepo.findAnswersBySessionId(sessionId);
        int totalScore = answers.stream()
                .mapToInt(a -> a.getScore() == null ? 0 : a.getScore())
                .sum();

        session.setScore(totalScore);
        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());

        InterviewSession saved = sessionRepo.save(session);

        // Day 12: Update skill profile after completion
        try {
            skillProfileService.updateProfile(session.getUserEmail(), session.getRole());
        } catch (Exception e) {
            log.warn("Skill profile update failed (non-critical): {}", e.getMessage());
        }

        return saved;
    }

    /**
     * Batch evaluate all answers for a session at completion.
     */
    private void batchEvaluateAnswers(Long sessionId) {
        log.info("Batch evaluating answers for session {}", sessionId);

        List<Answer> answers = answerRepo.findAnswersBySessionId(sessionId);
        List<Question> questions = questionRepo.findBySessionIdOrderByQuestionOrder(sessionId);

        // Create a map of questionId to question for quick lookup
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        for (Answer answer : answers) {
            Question question = questionMap.get(answer.getQuestionId());
            if (question == null) {
                log.warn("Question not found for answer {}", answer.getId());
                continue;
            }

            String type = question.getType().toLowerCase();
            int score = 0;
            String aiFeedback = null;

            if ("mcq".equals(type)) {
                // Auto-score: compare selected answer to correct answer
                String expected = question.getCorrectAnswer() == null ? ""
                        : question.getCorrectAnswer().trim().toUpperCase();
                String given = answer.getUserAnswer().trim().toUpperCase();
                
                // Handle case where user submits full option text vs just the letter
                // If expected is a single letter (A, B, C, D), check if user answer starts with that letter
                if (expected.length() == 1 && Character.isLetter(expected.charAt(0))) {
                    // Expected is a single letter, check if user answer starts with it
                    if (given.startsWith(expected + ")") || given.startsWith(expected + ".") || given.equals(expected)) {
                        score = 1;
                    } else {
                        score = 0;
                    }
                } else {
                    // Expected is full text, do direct comparison
                    score = given.equals(expected) ? 1 : 0;
                }

            } else if ("subjective".equals(type)) {
                // AI evaluation via Flask → Ollama
                try {
                    String evalResult = aiClientService.evaluateSubjective(
                            question.getQuestionText(), answer.getUserAnswer());
                    // Extract score from JSON string e.g.: {"score": 7, ...}
                    int scoreIdx = evalResult.indexOf("\"score\"");
                    if (scoreIdx >= 0) {
                        String after = evalResult.substring(scoreIdx + 8).trim();
                        after = after.replaceAll("[^0-9].*", "").trim();
                        if (!after.isEmpty()) {
                            score = Math.min(10, Math.max(0, Integer.parseInt(after)));
                        }
                    }
                    aiFeedback = evalResult;
                } catch (Exception e) {
                    log.warn("AI evaluation failed, defaulting score to 0: {}", e.getMessage());
                }
            }

            // Update the answer with evaluation results
            answer.setScore(score);
            answer.setAiFeedback(aiFeedback);
            answerRepo.save(answer);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // GETTERS FOR CONTROLLER USE
    // ══════════════════════════════════════════════════════════════

    public List<Question> getQuestionsForSession(Long sessionId) {
        if (sessionId == null) {
            log.warn("Null session ID provided to getQuestionsForSession");
            return new ArrayList<>();
        }
        try {
            log.info("Attempting to retrieve questions for session ID: {}", sessionId);
            List<Question> questions = questionRepo.findBySessionIdOrderByQuestionOrder(sessionId);
            log.info("Retrieved {} questions for session ID: {}", questions.size(), sessionId);
            return questions;
        } catch (Exception e) {
            log.error("Error retrieving questions for session ID: {}", sessionId, e);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Answer> getAnswersForSession(Long sessionId) {
        if (sessionId == null) {
            log.warn("Null session ID provided to getAnswersForSession");
            return new ArrayList<>();
        }
        try {
            log.info("Attempting to retrieve answers for session ID: {}", sessionId);
            
            // First, check if the session exists
            Optional<InterviewSession> sessionOpt = sessionRepo.findById(sessionId);
            if (!sessionOpt.isPresent()) {
                log.warn("Session not found for ID: {}", sessionId);
                return new ArrayList<>();
            }
            
            List<Answer> answers = answerRepo.findAnswersBySessionId(sessionId);
            log.info("Retrieved {} answers for session ID: {}", answers.size(), sessionId);
            return answers;
        } catch (Exception e) {
            log.error("Error retrieving answers for session ID: {}", sessionId, e);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            // Return empty list instead of throwing exception
            return new ArrayList<>();
        }
    }

    public InterviewSession getSession(Long sessionId) {
        if (sessionId == null) {
            throw new RuntimeException("Session ID cannot be null");
        }
        try {
            return sessionRepo.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        } catch (Exception e) {
            log.error("Error retrieving session ID: {}", sessionId, e);
            throw new RuntimeException("Error retrieving session: " + e.getMessage());
        }
    }

    public Question generateAndSaveModelAnswer(Long sessionId, Long questionId) {
        log.info("generateAndSaveModelAnswer | sessionId={}, questionId={}", sessionId, questionId);
        Question question = questionRepo.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found: " + questionId));
        if (!sessionId.equals(question.getSessionId())) {
            throw new RuntimeException("Question does not belong to session: " + sessionId);
        }
        String model = aiClientService.generateModelAnswer(question.getQuestionText());
        question.setCorrectAnswer(model);
        return questionRepo.save(question);
    }
    /**
     * Generate AI follow-up question for subjective answers
     */
    public String generateFollowUp(String question, String answer) {
        try {
            // Simple follow-up generation - in production this would call AI service
            if (answer.length() < 50) {
                return "Could you elaborate more on your answer? Please provide specific details or examples.";
            }
            return "That's interesting. Can you explain the technical approach you would use and any potential challenges?";
        } catch (Exception e) {
            log.warn("Failed to generate follow-up question: {}", e.getMessage());
            return "Can you provide more details about your approach?";
        }
    }

    /**
     * Create fallback questions when AI service fails
     */
    private int createFallbackQuestions(InterviewSession session) {
        String type = session.getType().toLowerCase();
        String role = session.getRole();
        
        switch (type) {
            case "mcq":
                return createFallbackMCQQuestions(session, role);
            case "subjective":
                return createFallbackSubjectiveQuestions(session, role);
            case "full":
                // For full interviews, create both subjective and MCQ fallback questions
                int subjCount = createFallbackSubjectiveQuestions(session, role);
                int mcqCount = createFallbackMCQQuestions(session, role);
                return subjCount + mcqCount;
            default:
                return createFallbackSubjectiveQuestions(session, role);
        }
    }

    private int createFallbackMCQQuestions(InterviewSession session, String role) {
        String[][] questions = getFallbackMCQForRole(role);
        int count = 0;
        
        for (int i = 0; i < questions.length; i++) {
            Question q = Question.builder()
                    .sessionId(session.getId())
                    .questionText(questions[i][0])
                    .type("mcq")
                    .correctAnswer(questions[i][1])
                    .options(questions[i][2])
                    .questionOrder(i + 1)
                    .build();
            questionRepo.save(q);
            count++;
        }
        
        return count;
    }

    private int createFallbackSubjectiveQuestions(InterviewSession session, String role) {
        String[] questions = getFallbackSubjectiveForRole(role);
        int count = 0;
        
        for (int i = 0; i < questions.length; i++) {
            Question q = Question.builder()
                    .sessionId(session.getId())
                    .questionText(questions[i])
                    .type("subjective")
                    .questionOrder(i + 1)
                    .build();
            questionRepo.save(q);
            count++;
        }
        
        return count;
    }

    private int createFallbackCodingQuestions(InterviewSession session, String role) {
        String[] questions = getFallbackCodingForRole(role);
        int count = 0;
        
        for (int i = 0; i < questions.length; i++) {
            Question q = Question.builder()
                    .sessionId(session.getId())
                    .questionText(questions[i])
                    .type("coding")
                    .questionOrder(i + 1)
                    .build();
            questionRepo.save(q);
            count++;
        }
        
        return count;
    }

    private String[][] getFallbackMCQForRole(String role) {
        if (role.contains("Java")) {
            return new String[][]{
                {"What is the difference between == and .equals() in Java?", "A", "[\"== compares object references\", \".equals() compares object values\", \"Both are the same\", \"None of the above\"]"},
                {"Which keyword is used to inherit a class in Java?", "extends", "[\"extends\", \"implements\", \"inherits\", \"super\"]"},
                {"What is the default value of a boolean variable in Java?", "false", "[\"true\", \"false\", \"null\", \"0\"]"},
                {"Which exception is thrown when you divide by zero?", "ArithmeticException", "[\"NullPointerException\", \"ArithmeticException\", \"IOException\", \"ArrayIndexOutOfBoundsException\"]"},
                {"What is the purpose of the 'final' keyword in Java?", "To make constants and prevent inheritance", "[\"To make variables mutable\", \"To make constants and prevent inheritance\", \"To create abstract classes\", \"To handle exceptions\"]"}
            };
        }
        // Default fallback questions
        return new String[][]{
            {"What is the primary purpose of this role?", "A", "[\"A\", \"B\", \"C\", \"D\"]"},
            {"Which technology is most relevant?", "B", "[\"A\", \"B\", \"C\", \"D\"]"},
            {"What is a best practice in this field?", "C", "[\"A\", \"B\", \"C\", \"D\"]"},
            {"Which tool would you use for this task?", "D", "[\"A\", \"B\", \"C\", \"D\"]"},
            {"What is the most important skill?", "A", "[\"A\", \"B\", \"C\", \"D\"]"}
        };
    }

    private String[] getFallbackSubjectiveForRole(String role) {
        if (role.contains("Java")) {
            return new String[]{
                "Explain the concept of Object-Oriented Programming and its main principles.",
                "What is the difference between an interface and an abstract class in Java?",
                "How does garbage collection work in Java?",
                "Explain the Spring Boot framework and its key features.",
                "What design patterns have you used in your Java projects?"
            };
        }
        return new String[]{
            "Describe your experience with this role.",
            "What are the key responsibilities in this position?",
            "How do you stay updated with industry trends?",
            "Describe a challenging project you worked on.",
            "What are your career goals in this field?"
        };
    }

    private String[] getFallbackCodingForRole(String role) {
        if (role.contains("Java")) {
            return new String[]{
                "Write a Java method to reverse a string.",
                "Implement a binary search algorithm in Java.",
                "Create a Java class that represents a Bank Account with deposit and withdraw methods.",
                "Write a Java program to find the factorial of a number using recursion.",
                "Implement a simple REST controller in Spring Boot."
            };
        }
        return new String[]{
            "Write a function to check if a number is prime.",
            "Implement a sorting algorithm of your choice.",
            "Create a class to represent a User with validation.",
            "Write a function to find the largest element in an array.",
            "Implement a simple calculator with basic operations."
        };
    }
    
    // ══════════════════════════════════════════════════════════════
    //  DAY 10 – CODING EVALUATION WITH TEST CASES
    // ══════════════════════════════════════════════════════════════
    

}
