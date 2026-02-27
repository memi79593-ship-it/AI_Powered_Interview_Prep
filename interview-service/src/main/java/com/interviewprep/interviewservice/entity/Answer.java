package com.interviewprep.interviewservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores a user's submitted answer for a specific question in a session.
 * Score is set after evaluation (MCQ auto-scored, subjective AI-scored).
 */
@Entity
@Table(name = "answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Session this answer belongs to. */
    @Column(nullable = false)
    private Long sessionId;

    /** The question being answered. */
    @Column(nullable = false)
    private Long questionId;

    /** User's submitted answer text. */
    @Column(columnDefinition = "TEXT")
    private String userAnswer;

    /**
     * Score earned for this answer.
     * MCQ: 0 or 1 | Subjective: 0–10 | Coding: 0 or 1
     */
    @Builder.Default
    private Integer score = 0;

    /** AI feedback text for subjective answers. */
    @Column(columnDefinition = "TEXT")
    private String aiFeedback;
}
