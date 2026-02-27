package com.interviewprep.interviewservice.service;

import com.interviewprep.interviewservice.dto.DashboardDTO;
import com.interviewprep.interviewservice.entity.InterviewSession;
import com.interviewprep.interviewservice.repository.InterviewSessionRepository;
import com.interviewprep.interviewservice.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Day 7 – Score Calculation & Dashboard Analytics Service.
 *
 * Provides:
 * - Aggregated stats per user
 * - Role-wise average score breakdown
 * - Weakness detection (roles with avg < threshold)
 * - Recent sessions list
 */
@Service
public class ScoreService {

    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);

    /** Roles with average percentage below this threshold are flagged as weak. */
    private static final double WEAK_THRESHOLD_PERCENT = 50.0;

    private final InterviewSessionRepository sessionRepo;
    private final QuestionRepository questionRepo;

    public ScoreService(InterviewSessionRepository sessionRepo, QuestionRepository questionRepo) {
        this.sessionRepo = sessionRepo;
        this.questionRepo = questionRepo;
    }

    /**
     * Builds the complete analytics dashboard for a given user.
     *
     * @param email User's email
     * @return DashboardDTO with all analytics
     */
    public DashboardDTO buildDashboard(String email) {
        log.info("Building dashboard for user: {}", email);

        List<InterviewSession> sessions = sessionRepo.findByUserEmailOrderByCreatedAtDesc(email);

        // Filter only completed sessions for score analytics
        List<InterviewSession> completed = sessions.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .toList();

        DashboardDTO dto = new DashboardDTO();
        dto.setUserEmail(email);
        dto.setTotalSessions(sessions.size());

        if (completed.isEmpty()) {
            dto.setAverageScore(0.0);
            dto.setHighestScore(0);
            dto.setBestRole("N/A");
            dto.setWeakestRole("N/A");
            dto.setWeakAreas(List.of());
            dto.setRoleWiseAverage(Map.of());
            dto.setRecentSessions(buildRecentSessions(sessions));
            return dto;
        }

        // ── Average Score ────────────────────────────────────────
        double avgPct = completed.stream()
                .filter(s -> s.getTotalQuestions() != null && s.getTotalQuestions() > 0)
                .mapToDouble(this::calculatePercentageScore)
                .average()
                .orElse(0.0);
        dto.setAverageScore(Math.min(100.0, Math.round(avgPct * 10.0) / 10.0));

        // ── Highest Score ────────────────────────────────────────
        int highest = completed.stream()
                .mapToInt(s -> s.getScore() == null ? 0 : s.getScore())
                .max().orElse(0);
        dto.setHighestScore(highest);

        // ── Role-wise Average Score ──────────────────────────────
        Map<String, Double> roleWiseAvg = completed.stream()
                .collect(Collectors.groupingBy(
                        InterviewSession::getRole,
                        Collectors.averagingDouble(
                                this::calculatePercentageScore)));

        // Round to 2 decimal places
        roleWiseAvg.replaceAll((k, v) -> Math.min(100.0, Math.round(v * 100.0) / 100.0));
        dto.setRoleWiseAverage(roleWiseAvg);

        // ── Best Role (highest average) ──────────────────────────
        String bestRole = roleWiseAvg.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        dto.setBestRole(bestRole);

        // ── Weakest Role (lowest average) ───────────────────────
        String weakestRole = roleWiseAvg.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        dto.setWeakestRole(weakestRole);

        // ── Weak Areas Detection (avg < 50%) ─────────────────────
        List<String> weakAreas = roleWiseAvg.entrySet().stream()
                .filter(e -> e.getValue() < WEAK_THRESHOLD_PERCENT)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        dto.setWeakAreas(weakAreas);

        // ── Last Interview Date ──────────────────────────────────
        sessions.stream()
                .findFirst() // already sorted desc
                .ifPresent(s -> dto.setLastInterviewDate(
                        s.getCreatedAt() != null ? s.getCreatedAt().toString() : "N/A"));

        // ── Recent Sessions (last 5) ─────────────────────────────
        dto.setRecentSessions(buildRecentSessions(sessions));

        return dto;
    }

    // ── Helpers ──────────────────────────────────────────────────

    /** Returns a summary map for each of the last 5 sessions. */
    private List<Map<String, Object>> buildRecentSessions(List<InterviewSession> sessions) {
        return sessions.stream().limit(5).map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sessionId", s.getId());
            map.put("role", s.getRole());
            map.put("type", s.getType());
            map.put("difficulty", s.getDifficulty());
            map.put("score", s.getScore() == null ? 0 : s.getScore());
            map.put("status", s.getStatus());
            map.put("date", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
            return map;
        }).toList();
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
