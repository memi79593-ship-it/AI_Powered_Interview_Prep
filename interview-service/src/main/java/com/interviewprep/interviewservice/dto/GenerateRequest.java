package com.interviewprep.interviewservice.dto;

/**
 * Request DTO for generating interview questions.
 *
 * Example JSON body:
 * {
 *   "role": "Java Developer",
 *   "level": "medium"
 * }
 */
public class GenerateRequest {

    /** Job role for the interview (e.g., "Java Developer", "DevOps Engineer"). */
    private String role;

    /** Difficulty level: easy | medium | hard */
    private String level;

    // ──────────────────────────────────────────────
    // Getters & Setters (manual – no Lombok on DTOs)
    // ──────────────────────────────────────────────

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "GenerateRequest{role='" + role + "', level='" + level + "'}";
    }
}
