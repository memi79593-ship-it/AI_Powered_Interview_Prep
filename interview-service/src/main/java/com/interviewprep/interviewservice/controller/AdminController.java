package com.interviewprep.interviewservice.controller;

import com.interviewprep.interviewservice.entity.AppUser;
import com.interviewprep.interviewservice.entity.InterviewSession;
import com.interviewprep.interviewservice.repository.AppUserRepository;
import com.interviewprep.interviewservice.repository.InterviewSessionRepository;
import com.interviewprep.interviewservice.repository.QuestionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Day 15 – Admin Controller.
 * Routes are protected by ROLE_ADMIN in SecurityConfig.
 *
 * GET /api/admin/users – All registered users + session counts
 * GET /api/admin/analytics – Platform-wide metrics
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AppUserRepository userRepo;
    private final InterviewSessionRepository sessionRepo;
    private final QuestionRepository questionRepo;

    public AdminController(AppUserRepository userRepo,
            InterviewSessionRepository sessionRepo,
            QuestionRepository questionRepo) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.questionRepo = questionRepo;
    }

    /** GET /api/admin/users – user list with session stats. */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        List<AppUser> users = userRepo.findAll();
        List<Map<String, Object>> result = users.stream().map(u -> {
            long total = sessionRepo.countByUserEmail(u.getEmail());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("active", u.isActive());
            m.put("totalSessions", total);
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** GET /api/admin/analytics – platform-wide aggregate statistics. */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getPlatformAnalytics() {
        List<InterviewSession> all = sessionRepo.findAll();
        long total = all.size();
        long completed = all.stream().filter(s -> "COMPLETED".equals(s.getStatus())).count();
        long users = userRepo.count();

        double avgScore = all.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus())
                        && s.getTotalQuestions() != null && s.getTotalQuestions() > 0)
                .mapToDouble(this::calculatePercentageScore)
                .average().orElse(0.0);

        // Most popular role
        Optional<Map.Entry<String, Long>> topRole = all.stream()
                .filter(s -> s.getRole() != null)
                .collect(Collectors.groupingBy(InterviewSession::getRole, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue());

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("totalUsers", users);
        analytics.put("totalSessions", total);
        analytics.put("completedSessions", completed);
        analytics.put("averageScore", Math.min(100.0, Math.round(avgScore * 10.0) / 10.0));
        analytics.put("mostPopularRole", topRole.map(Map.Entry::getKey).orElse("N/A"));
        analytics.put("completionRate", total > 0 ? Math.round(completed * 100.0 / total) : 0);
        return ResponseEntity.ok(analytics);
    }

    private double calculatePercentageScore(InterviewSession session) {
        try {
            var questions = questionRepo.findBySessionIdOrderByQuestionOrder(session.getId());
            if (questions == null || questions.isEmpty()) {
                return session.getTotalQuestions() != null && session.getTotalQuestions() > 0
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
            return session.getTotalQuestions() != null && session.getTotalQuestions() > 0
                    ? (session.getScore() * 100.0) / session.getTotalQuestions()
                    : 0.0;
        }
    }
}
