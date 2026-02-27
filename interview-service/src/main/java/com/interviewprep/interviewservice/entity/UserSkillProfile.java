package com.interviewprep.interviewservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Day 12 – User Skill Profile.
 *
 * Maintained after every completed interview session.
 * Stores cumulative performance metrics per user+role pair.
 * Used for adaptive difficulty, weak-topic detection, and confidence scoring.
 */
@Entity
@Table(name = "user_skill_profiles", uniqueConstraints = @UniqueConstraint(columnNames = { "userEmail", "role" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String role;

    @Builder.Default
    private double avgScore = 0.0;

    /** Score % from last 3 sessions (trend indicator). */
    @Builder.Default
    private double recentTrend = 0.0;

    /**
     * Confidence score (0–100).
     * confidence = 0.7 * avgScore + 0.3 * recentTrend
     */
    @Builder.Default
    private double confidenceScore = 0.0;

    /** HIGH | MEDIUM | LOW */
    @Builder.Default
    private String confidenceLevel = "LOW";

    /** Comma-separated weak topics (avg < 50%). */
    @Column(columnDefinition = "TEXT")
    private String weakTopics;

    /** Comma-separated strong topics (avg >= 75%). */
    @Column(columnDefinition = "TEXT")
    private String strongTopics;

    @Builder.Default
    private int totalSessions = 0;

    @Builder.Default
    private int totalCompleted = 0;

    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
