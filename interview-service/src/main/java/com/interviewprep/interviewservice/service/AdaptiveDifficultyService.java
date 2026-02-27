package com.interviewprep.interviewservice.service;

import com.interviewprep.interviewservice.repository.InterviewSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Day 9 – Adaptive Difficulty Engine.
 *
 * Determines the recommended difficulty for a user's NEXT interview session
 * based on their historical average score.
 *
 * Rules:
 * avg ≥ 80% → Hard
 * avg ≥ 50% → Medium
 * avg < 50% → Easy
 */
@Service
public class AdaptiveDifficultyService {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveDifficultyService.class);

    private static final double HARD_THRESHOLD = 80.0;
    private static final double MEDIUM_THRESHOLD = 50.0;

    private final InterviewSessionRepository sessionRepo;

    public AdaptiveDifficultyService(InterviewSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    /**
     * Recommend difficulty for the next session based on past performance.
     *
     * @param userEmail User's email
     * @param role      Role being interviewed for (filters by role)
     * @return "easy" | "medium" | "hard"
     */
    public String recommendDifficulty(String userEmail, String role) {
        var sessions = sessionRepo.findByUserEmailAndRole(userEmail, role)
                .stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .filter(s -> s.getScore() != null && s.getTotalQuestions() != null
                        && s.getTotalQuestions() > 0)
                .toList();

        if (sessions.isEmpty()) {
            log.info("No history for user={} role={} → default: medium", userEmail, role);
            return "medium";
        }

        // Calculate average percentage score across completed sessions
        double avgPercent = sessions.stream()
                .mapToDouble(s -> (s.getScore() * 100.0) / s.getTotalQuestions())
                .average()
                .orElse(50.0);

        String difficulty;
        if (avgPercent >= HARD_THRESHOLD) {
            difficulty = "hard";
        } else if (avgPercent >= MEDIUM_THRESHOLD) {
            difficulty = "medium";
        } else {
            difficulty = "easy";
        }

        log.info("Adaptive difficulty | user={} role={} avgPct={:.1f}% → {}",
                userEmail, role, avgPercent, difficulty);
        return difficulty;
    }
}
