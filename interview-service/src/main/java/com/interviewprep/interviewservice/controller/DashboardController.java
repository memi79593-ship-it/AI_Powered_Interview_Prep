package com.interviewprep.interviewservice.controller;

import com.interviewprep.interviewservice.dto.DashboardDTO;
import com.interviewprep.interviewservice.service.AdaptiveDifficultyService;
import com.interviewprep.interviewservice.service.ScoreService;
import com.interviewprep.interviewservice.service.WeakTopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

/**
 * Day 11 – Performance Summary & Adaptive Recommendation Controller.
 *
 * GET /api/dashboard/{email} – Analytics (existing)
 * GET /api/dashboard/{email}/recommend/{role} – Recommended difficulty
 * GET /api/dashboard/{email}/wtopics/{sessionId} – Weak topics for session
 * POST /api/dashboard/{email}/summary – AI performance summary
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final ScoreService scoreService;
    private final AdaptiveDifficultyService adaptiveService;
    private final WeakTopicService weakTopicService;
    private final RestTemplate restTemplate;

    @Value("${flask.ai.base-url:http://localhost:5000}")
    private String flaskUrl;

    public DashboardController(ScoreService scoreService,
            AdaptiveDifficultyService adaptiveService,
            WeakTopicService weakTopicService,
            RestTemplate restTemplate) {
        this.scoreService = scoreService;
        this.adaptiveService = adaptiveService;
        this.weakTopicService = weakTopicService;
        this.restTemplate = restTemplate;
    }

    /** GET /api/dashboard/{email} – Full analytics. */
    @GetMapping("/{email}")
    public ResponseEntity<DashboardDTO> getDashboard(@PathVariable String email) {
        return ResponseEntity.ok(scoreService.buildDashboard(email));
    }

    /**
     * GET /api/dashboard/{email}/recommend/{role}
     * Day 9: Returns recommended difficulty for user's next session.
     * Response: { "email": "...", "role": "...", "recommendedDifficulty": "hard" }
     */
    @GetMapping("/{email}/recommend/{role}")
    public ResponseEntity<Map<String, String>> recommendDifficulty(
            @PathVariable String email,
            @PathVariable String role) {
        String difficulty = adaptiveService.recommendDifficulty(email, role);
        return ResponseEntity.ok(Map.of(
                "email", email,
                "role", role,
                "recommendedDifficulty", difficulty));
    }

    /**
     * GET /api/dashboard/{email}/wtopics/{sessionId}
     * Day 9: Returns weak topics detected in a completed session.
     */
    @GetMapping("/{email}/wtopics/{sessionId}")
    public ResponseEntity<Map<String, Object>> getWeakTopics(
            @PathVariable String email,
            @PathVariable Long sessionId) {
        List<String> weakTopics = weakTopicService.getWeakTopics(sessionId);
        Map<String, Double> topicScores = weakTopicService.getTopicScores(sessionId);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "topicScores", topicScores,
                "weakTopics", weakTopics));
    }

    /**
     * POST /api/dashboard/{email}/summary
     * Day 11: Calls Flask to generate AI performance summary + roadmap.
     *
     * Body: { "role": "...", "score": 6, "total": 10,
     * "weakTopics": ["OOP"], "strongTopics": ["Collections"] }
     */
    @PostMapping("/{email}/summary")
    public ResponseEntity<Map> getPerformanceSummary(
            @PathVariable String email,
            @RequestBody Map<String, Object> body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                flaskUrl + "/generate-performance-summary",
                HttpMethod.POST, req,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });

        return ResponseEntity.ok(response.getBody());
    }
}
