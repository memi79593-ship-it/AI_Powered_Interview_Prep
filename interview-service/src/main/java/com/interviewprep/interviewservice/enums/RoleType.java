package com.interviewprep.interviewservice.enums;

/**
 * Supported interview roles.
 *
 * FULL AI SUPPORT → Subjective + MCQ + Coding questions
 * PARTIAL AI SUPPORT → Subjective + MCQ only
 */
public enum RoleType {

    // ── Full AI Support ─────────────────────────────────
    JAVA_DEVELOPER,
    PYTHON_DEVELOPER,
    C_PROGRAMMER,
    CPP_PROGRAMMER,

    // ── Partial AI Support (no coding questions) ─────────
    DEVOPS_ENGINEER,
    QA_ENGINEER,
    DATA_ANALYST,
    WEB_DEVELOPER,

    // ── Day 11: New Roles (MCQ + Subjective only) ─────────
    FRONTEND_DEVELOPER,
    BACKEND_DEVELOPER,
    MOBILE_DEVELOPER,
    DATABASE_ADMINISTRATOR,
    CLOUD_ENGINEER
}
