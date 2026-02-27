package com.interviewprep.interviewservice.controller;

import com.interviewprep.interviewservice.dto.*;
import com.interviewprep.interviewservice.service.AIClientService;
import com.interviewprep.interviewservice.service.InterviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for interview-related endpoints.
 *
 * Base path: /api/interview
 *
 * Endpoints (Days 1–5):
 *   POST /generate-subjective      Day 1 – Generate open-ended questions
 *   POST /generate-mcq             Day 3 – Generate multiple-choice questions
 *   POST /evaluate-mcq             Day 3 – Auto-score MCQ answers
 *   POST /generate-coding          Day 4 – Generate coding challenges
 *   POST /execute-code             Day 4 – Run code via JDoodle
 *   POST /evaluate-subjective      Day 5 – AI-score a subjective answer
 *   GET  /health                         – Health check
 */
@RestController
@RequestMapping("/api/interview")
@CrossOrigin(origins = "*")
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final AIClientService aiService;
    private final InterviewService interviewService;

    public InterviewController(AIClientService aiService, InterviewService interviewService) {
        this.aiService = aiService;
        this.interviewService = interviewService;
    }

    // ══════════════════════════════════════════════════════════════
    //  HEALTH
    // ══════════════════════════════════════════════════════════════

    /** GET /api/interview/health */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "interview-service"));
    }

    // ══════════════════════════════════════════════════════════════
    //  DAY 1 – SUBJECTIVE
    // ══════════════════════════════════════════════════════════════

    /**
     * POST /api/interview/generate-subjective
     * Body: { "role": "Java Developer", "level": "medium" }
     */
    @PostMapping("/generate-subjective")
    public ResponseEntity<Map<String, Object>> generateSubjective(
            @RequestBody GenerateRequest request) {

        log.info("→ generateSubjective {}", request);
        try {
            String result = aiService.generateSubjective(request.getRole(), request.getLevel());
            return ResponseEntity.ok(Map.of(
                    "success",   true,
                    "role",      request.getRole(),
                    "level",     request.getLevel(),
                    "questions", result
            ));
        } catch (Exception e) {
            log.error("generateSubjective failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false, "error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DAY 3 – MCQ GENERATION
    // ══════════════════════════════════════════════════════════════

    /**
     * POST /api/interview/generate-mcq
     * Body: { "role": "Java Developer", "level": "medium" }
     */
    @PostMapping("/generate-mcq")
    public ResponseEntity<Map<String, Object>> generateMCQ(
            @RequestBody GenerateRequest request) {

        log.info("→ generateMCQ {}", request);
        try {
            String result = aiService.generateMCQ(request.getRole(), request.getLevel());
            return ResponseEntity.ok(Map.of(
                    "success",   true,
                    "role",      request.getRole(),
                    "level",     request.getLevel(),
                    "questions", result
            ));
        } catch (Exception e) {
            log.error("generateMCQ failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false, "error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DAY 3 – MCQ EVALUATION
    // ══════════════════════════════════════════════════════════════

    /**
     * POST /api/interview/evaluate-mcq
     *
     * Body:
     * {
     *   "questions": [
     *     {
     *       "question": "...",
     *       "options": [...],
     *       "correctAnswer": "A",
     *       "selectedAnswer": "A"
     *     }
     *   ]
     * }
     *
     * Response: { score, total, percentage, results[] }
     */
    @PostMapping("/evaluate-mcq")
    public ResponseEntity<Map<String, Object>> evaluateMCQ(
            @RequestBody MCQEvaluateRequest request) {

        log.info("→ evaluateMCQ | {} questions", request.getQuestions().size());
        try {
            // Convert typed DTOs to raw maps for Flask
            List<Map<String, Object>> questions = request.getQuestions().stream()
                    .map(q -> Map.of(
                            "question",      (Object) q.getQuestion(),
                            "options",       q.getOptions(),
                            "correctAnswer", q.getCorrectAnswer(),
                            "selectedAnswer", q.getSelectedAnswer()
                    ))
                    .toList();

            Map<String, Object> result = aiService.evaluateMCQ(questions);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("evaluateMCQ failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false, "error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  CODING FUNCTIONALITY REMOVED
    // ══════════════════════════════════════════════════════════════
    // Coding question generation and execution endpoints removed

    // ══════════════════════════════════════════════════════════════
    //  CODING EVALUATION REMOVED
    // ══════════════════════════════════════════════════════════════
    // Coding evaluation endpoint removed: coding functionality no longer supported
    // ══════════════════════════════════════════════════════════════

    /**
     * POST /api/interview/evaluate-subjective
     *
     * Body: { "question": "...", "answer": "..." }
     *
     * Response:
     * {
     *   "success": true,
     *   "evaluation": {
     *     "score": 8,
     *     "technicalAccuracy": 9,
     *     "completeness": 7,
     *     "clarity": 8,
     *     "bestPractices": 8,
     *     "depth": 7,
     *     "feedback": "...",
     *     "improvements": "..."
     *   }
     * }
     */
    @PostMapping("/evaluate-subjective")
    public ResponseEntity<Map<String, Object>> evaluateSubjective(
            @RequestBody EvaluateSubjectiveRequest request) {

        log.info("→ evaluateSubjective {}", request);
        try {
            String result = aiService.evaluateSubjective(
                    request.getQuestion(),
                    request.getAnswer()
            );
            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "evaluation", result
            ));
        } catch (Exception e) {
            log.error("evaluateSubjective failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false, "error", e.getMessage()));
        }
    }
}
