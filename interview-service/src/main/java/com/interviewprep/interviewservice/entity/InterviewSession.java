package com.interviewprep.interviewservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a single interview session lifecycle.
 * status: STARTED → IN_PROGRESS → COMPLETED
 * type: subjective | mcq | coding | full
 */
@Entity
@Table(name = "interview_sessions", indexes = @Index(name = "idx_user_email", columnList = "userEmail"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email of the user who started this session. */
    @Email
    @Column(nullable = false)
    private String userEmail;

    /** Role attempted (e.g., "Java Developer"). */
    @NotBlank
    @Column(nullable = false)
    private String role;

    /** Interview type: subjective | mcq | coding | full */
    @Column(nullable = false)
    private String type;

    /** Difficulty: easy | medium | hard */
    @Column(nullable = false)
    private String difficulty;

    /** Number of questions requested by user (default: 5) */
    @Builder.Default
    private Integer questionCount = 5;

    /** Total questions generated for this session. */
    private Integer totalQuestions;

    /** Final score achieved (updated on completion). */
    @Builder.Default
    private Integer score = 0;

    /** STARTED | IN_PROGRESS | COMPLETED */
    @Builder.Default
    private String status = "STARTED";

    /** Session creation timestamp. */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** When the session was completed. */
    private LocalDateTime completedAt;
}
