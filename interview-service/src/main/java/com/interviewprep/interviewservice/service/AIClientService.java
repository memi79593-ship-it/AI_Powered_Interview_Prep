package com.interviewprep.interviewservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for calling the Flask AI microservice,
 * which in turn communicates with Ollama (local LLM) and JDoodle.
 *
 * Flow:
 * Spring Boot (this) → Flask (localhost:5000) → Ollama / JDoodle
 *
 * Supported operations (Days 1–5):
 * - generateSubjective → /generate-subjective (Day 1/9)
 * - generateMCQ → /generate-mcq (Day 3/9)
 * - evaluateMCQ → /evaluate-mcq (Day 3)
 * - generateCoding → /generate-coding (Day 4/9)
 * - executeCode → /execute-code (Day 4/9)
 * - evaluateSubjective → /evaluate-subjective (Day 5/9)
 */
@Service
public class AIClientService {

    private static final Logger log = LoggerFactory.getLogger(AIClientService.class);

    private final RestTemplate restTemplate;

    @Value("${flask.ai.base-url:http://localhost:5000}")
    private String flaskBaseUrl;

    public AIClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════

    /** Build a POST request with JSON content-type. */
    private <T> HttpEntity<T> jsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    /** POST to Flask and extract the "data" field from the response. */
    private String postAndExtractData(String path, Object body) {
        String url = flaskBaseUrl + path;
        log.debug("→ Flask POST {}", url);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, jsonEntity(body), Map.class);

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from Flask at " + path);
            }
            Object data = responseBody.get("data");
            if (data == null) {
                throw new RuntimeException("No 'data' field in Flask response: " + responseBody);
            }
            return data.toString();

        } catch (ResourceAccessException e) {
            String msg = "Flask AI service not reachable at " + flaskBaseUrl +
                    ". Ensure 'python app.py' is running.";
            log.error(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /** POST to Flask and return the entire raw response body as a Map. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postAndGetFullBody(String path, Object body) {
        String url = flaskBaseUrl + path;
        log.debug("→ Flask POST {}", url);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, jsonEntity(body), Map.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from Flask at " + path);
            }
            return (Map<String, Object>) response.getBody();

        } catch (ResourceAccessException e) {
            String msg = "Flask AI service not reachable at " + flaskBaseUrl;
            log.error(msg);
            throw new RuntimeException(msg, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DAY 1 – SUBJECTIVE GENERATION
    // ══════════════════════════════════════════════════════════════

    /**
     * Generate 5 subjective questions (basic, no weak topic).
     */
    public String generateSubjective(String role, String level) {
        return generateSubjective(role, level, 5, null);
    }

    /**
     * Day 9: Generate 5 subjective questions, optionally focused on a weak topic.
     *
     * @param role      e.g. "Java Developer"
     * @param level     e.g. "medium"
     * @param weakTopic e.g. "OOP" – if provided, AI focuses on this topic
     * @return Raw JSON string of 5 questions
     */
    public String generateSubjective(String role, String level, String weakTopic) {
        return generateSubjective(role, level, 5, weakTopic);
    }

    /**
     * Generate custom number of subjective questions with optional weak topic focus.
     *
     * @param role          e.g. "Java Developer"
     * @param level         e.g. "medium"
     * @param questionCount number of questions to generate
     * @param weakTopic     e.g. "OOP" – if provided, AI focuses on this topic
     * @return Raw JSON string of questions
     */
    public String generateSubjective(String role, String level, int questionCount, String weakTopic) {
        log.info("generateSubjective | role={}, level={}, questionCount={}, weakTopic={}", role, level, questionCount, weakTopic);
        Map<String, Object> body = new HashMap<>();
        body.put("role", role);
        body.put("level", level);
        body.put("questionCount", questionCount);
        if (weakTopic != null && !weakTopic.isBlank()) {
            body.put("weakTopic", weakTopic);
        }
        return postAndExtractData("/generate-subjective", body);
    }

    // ══════════════════════════════════════════════════════════════
    // DAY 3 – MCQ GENERATION & EVALUATION
    // ══════════════════════════════════════════════════════════════

    /**
     * Generate 5 MCQ questions (basic, no weak topic).
     */
    public String generateMCQ(String role, String level) {
        return generateMCQ(role, level, 5, null);
    }

    /**
     * Day 9: Generate 5 MCQ questions, optionally focused on a weak topic.
     */
    public String generateMCQ(String role, String level, String weakTopic) {
        return generateMCQ(role, level, 5, weakTopic);
    }

    /**
     * Generate custom number of MCQ questions with optional weak topic focus.
     *
     * @param role          e.g. "Java Developer"
     * @param level         e.g. "medium"
     * @param questionCount number of questions to generate
     * @param weakTopic     e.g. "OOP" – if provided, AI focuses on this topic
     * @return Raw JSON string of questions
     */
    public String generateMCQ(String role, String level, int questionCount, String weakTopic) {
        log.info("generateMCQ | role={}, level={}, questionCount={}, weakTopic={}", role, level, questionCount, weakTopic);
        Map<String, Object> body = new HashMap<>();
        body.put("role", role);
        body.put("level", level);
        body.put("questionCount", questionCount);
        if (weakTopic != null && !weakTopic.isBlank()) {
            body.put("weakTopic", weakTopic);
        }
        return postAndExtractData("/generate-mcq", body);
    }

    /**
     * Evaluate submitted MCQ answers automatically (no AI needed).
     *
     * @param questions List of maps, each with question, correctAnswer,
     *                  selectedAnswer
     * @return Map with score, total, percentage, results[]
     */
    public Map<String, Object> evaluateMCQ(List<Map<String, Object>> questions) {
        log.info("evaluateMCQ | questions={}", questions.size());
        Map<String, Object> body = new HashMap<>();
        body.put("questions", questions);
        return postAndGetFullBody("/evaluate-mcq", body);
    }

    // ══════════════════════════════════════════════════════════════
    // CODING FUNCTIONALITY REMOVED
    // ══════════════════════════════════════════════════════════════
    // Coding question generation and code execution methods removed

    // ══════════════════════════════════════════════════════════════
    // DAY 5 – SUBJECTIVE ANSWER EVALUATION (AI Scoring)
    // ══════════════════════════════════════════════════════════════

    /**
     * Use Ollama to evaluate and score a subjective interview answer.
     * Returns a JSON object with score/10 and detailed dimensional feedback.
     *
     * @param question The interview question
     * @param answer   The candidate's answer
     * @return Raw JSON string: {score, technicalAccuracy, completeness, clarity,
     *         bestPractices, depth, feedback, improvements}
     */
    public String evaluateSubjective(String question, String answer) {
        log.info("evaluateSubjective | question snippet='{}'",
                question.length() > 50 ? question.substring(0, 50) + "..." : question);
        Map<String, String> body = new HashMap<>();
        body.put("question", question);
        body.put("answer", answer);
        return postAndExtractData("/evaluate-subjective", body);
    }

    public String generateModelAnswer(String question) {
        log.info("generateModelAnswer | question snippet='{}'",
                question.length() > 50 ? question.substring(0, 50) + "..." : question);
        Map<String, String> body = new HashMap<>();
        body.put("question", question);
        return postAndExtractData("/generate-model-answer", body);
    }
}
