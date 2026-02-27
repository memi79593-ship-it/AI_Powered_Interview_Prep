package com.interviewprep.interviewservice.controller;

import com.interviewprep.interviewservice.dto.AnswerDTO;
import com.interviewprep.interviewservice.entity.Answer;
import com.interviewprep.interviewservice.entity.InterviewSession;
import com.interviewprep.interviewservice.entity.Question;
import com.interviewprep.interviewservice.service.InterviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Session lifecycle controller – Day 6.
 *
 * POST /api/session/start – Start a new interview session
 * GET /api/session/{id}/questions – Get questions for a session
 * POST /api/session/submit-answer – Submit an answer for evaluation
 * POST /api/session/complete/{id} – Finalise session and calc score
 * GET /api/session/{id} – Get session details
 */
@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*")
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final InterviewService interviewService;

    public SessionController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /**
     * POST /api/session/start
     * Body: { "userEmail": "a@b.com", "role": "Java Developer",
     * "type": "mcq", "difficulty": "medium" }
     */
    @PostMapping("/start")
    public ResponseEntity<InterviewSession> startInterview(
            @Valid @RequestBody InterviewSession request) {
        log.info("Start interview request: {}", request.getUserEmail());
        return ResponseEntity.ok(interviewService.startInterview(request));
    }

    /**
     * GET /api/session/{id}/questions
     * Returns all questions for a session in order.
     */
    @GetMapping("/{id}/questions")
    public ResponseEntity<List<Question>> getQuestions(@PathVariable Long id) {
        try {
            log.info("Fetching questions for session ID: {}", id);
            List<Question> questions = interviewService.getQuestionsForSession(id);
            log.info("Found {} questions for session ID: {}", questions.size(), id);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            log.error("Error fetching questions for session ID: {}", id, e);
            log.error("Error details - Type: {}, Message: {}", e.getClass().getName(), e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * POST /api/session/submit-answer
     * Body: { "sessionId": 1, "questionId": 5, "answer": "..." }
     */
    @PostMapping("/submit-answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @Valid @RequestBody AnswerDTO dto) {
        log.info("Submit answer | session={}, question={}", dto.getSessionId(), dto.getQuestionId());
        var answer = interviewService.submitAnswer(dto);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "answerId", answer.getId(),
                "score", answer.getScore(),
                "feedback", answer.getAiFeedback() != null ? answer.getAiFeedback() : ""));
    }

    /**
     * POST /api/session/complete/{sessionId}
     * Aggregates scores and marks session COMPLETED.
     */
    @PostMapping("/complete/{sessionId}")
    public ResponseEntity<InterviewSession> completeInterview(
            @PathVariable Long sessionId) {
        log.info("Complete session {}", sessionId);
        return ResponseEntity.ok(interviewService.completeInterview(sessionId));
    }

    /**
     * GET /api/session/{id}
     * Returns session metadata.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InterviewSession> getSession(@PathVariable Long id) {
        try {
            log.info("Fetching session ID: {}", id);
            InterviewSession session = interviewService.getSession(id);
            log.info("Found session: {} with status: {}", session.getId(), session.getStatus());
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            log.error("Error fetching session ID: {}", id, e);
            // Return 404 if session not found, 500 for other errors
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /api/session/{id}/answers
     * Returns all answers for a session with evaluation results.
     */
    @GetMapping("/{id}/answers")
    public ResponseEntity<List<Answer>> getAnswers(@PathVariable Long id) {
        try {
            log.info("Fetching answers for session ID: {}", id);
            List<Answer> answers = interviewService.getAnswersForSession(id);
            log.info("Found {} answers for session ID: {}", answers.size(), id);
            return ResponseEntity.ok(answers);
        } catch (Exception e) {
            log.error("Error fetching answers for session ID: {}", id, e);
            log.error("Error details - Type: {}, Message: {}", e.getClass().getName(), e.getMessage());
            // Return empty list instead of error to prevent 500
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/{id}/questions/{questionId}/model-answer")
    public ResponseEntity<Question> generateModelAnswer(
            @PathVariable Long id, @PathVariable Long questionId) {
        log.info("Generate model answer | session={}, question={}", id, questionId);
        try {
            Question updated = interviewService.generateAndSaveModelAnswer(id, questionId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Failed to generate model answer: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    /**
     * POST /api/session/{id}/followup
     * Body: { "question": "...", "answer": "..." }
     * Returns AI-generated follow-up question for subjective answers
     */
    @PostMapping("/{id}/followup")
    public ResponseEntity<Map<String, String>> getFollowUp(
            @PathVariable Long id, @RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = request.get("answer");
        
        if (question == null || answer == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "question and answer required"));
        }
        
        // For now, return a simple follow-up. In production, this would call AI service
        String followUp = interviewService.generateFollowUp(question, answer);
        return ResponseEntity.ok(Map.of("followUpQuestion", followUp));
    }
}
