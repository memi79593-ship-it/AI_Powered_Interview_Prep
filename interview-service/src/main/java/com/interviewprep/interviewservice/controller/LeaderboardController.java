package com.interviewprep.interviewservice.controller;

import com.interviewprep.interviewservice.entity.InterviewSession;
import com.interviewprep.interviewservice.repository.InterviewSessionRepository;
import com.interviewprep.interviewservice.repository.QuestionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Day 14 – Leaderboard Controller.
 *
 * GET /api/leaderboard – Top 10 overall
 * GET /api/leaderboard/{role} – Top 10 for a role
 */
@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private final InterviewSessionRepository sessionRepo;
    private final QuestionRepository questionRepo;

    public LeaderboardController(InterviewSessionRepository sessionRepo, QuestionRepository questionRepo) {
        this.sessionRepo = sessionRepo;
        this.questionRepo = questionRepo;
    }

    /** Top 10 users by average score percentage across all roles. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {
        return ResponseEntity.ok(buildLeaderboard(sessionRepo.findAll(), null));
    }

    /** Top 10 users by average score percentage for a specific role. */
    @GetMapping("/{role}")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboardByRole(
            @PathVariable String role) {
        List<InterviewSession> sessions = sessionRepo.findAll().stream()
                .filter(s -> role.equalsIgnoreCase(s.getRole()))
                .toList();
        return ResponseEntity.ok(buildLeaderboard(sessions, role));
    }

    private List<Map<String, Object>> buildLeaderboard(
            List<InterviewSession> sessions, String roleFilter) {

        Map<String, DoubleSummaryStatistics> stats = sessions.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .filter(s -> s.getTotalQuestions() != null && s.getTotalQuestions() > 0)
                .collect(Collectors.groupingBy(
                        InterviewSession::getUserEmail,
                        Collectors.summarizingDouble(
                                s -> calculatePercentageScore(s))));

        return stats.entrySet().stream()
                .sorted(Map.Entry.<String, DoubleSummaryStatistics>comparingByValue(
                        Comparator.comparingDouble(DoubleSummaryStatistics::getAverage).reversed()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("email", e.getKey());
                    double avg = e.getValue().getAverage();
                    entry.put("avgScore", Math.min(100.0, Math.round(avg * 10.0) / 10.0));
                    entry.put("totalSessions", (int) e.getValue().getCount());
                    if (roleFilter != null)
                        entry.put("role", roleFilter);
                    return entry;
                })
                .collect(Collectors.toList());
    }

    private double calculatePercentageScore(InterviewSession session) {
        try {
            var questions = questionRepo.findBySessionIdOrderByQuestionOrder(session.getId());
            if (questions == null || questions.isEmpty()) {
                return session.getTotalQuestions() > 0
                        ? (session.getScore() * 100.0) / session.getTotalQuestions()
                        : 0.0;
            }
            int maxPossible = 0;
            for (var q : questions) {
                if ("mcq".equalsIgnoreCase(q.getType()) || "coding".equalsIgnoreCase(q.getType())) {
                    maxPossible += 1;
                } else {
                    maxPossible += 10;
                }
            }
            return maxPossible > 0 ? (session.getScore() * 100.0) / maxPossible : 0.0;
        } catch (Exception e) {
            return session.getTotalQuestions() > 0
                    ? (session.getScore() * 100.0) / session.getTotalQuestions()
                    : 0.0;
        }
    }
}
