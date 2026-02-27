package com.interviewprep.interviewservice.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for the dashboard analytics endpoint.
 */
public class DashboardDTO {

    private String userEmail;
    private long totalSessions;
    private double averageScore;
    private int highestScore;
    private String bestRole;
    private String weakestRole;
    private String lastInterviewDate;

    /** Score per role: { "Java Developer": 7.5, "Python Developer": 4.0 } */
    private Map<String, Double> roleWiseAverage;

    /** Roles where average score < 50% → flagged as weak. */
    private List<String> weakAreas;

    /** Latest 5 sessions summary. */
    private List<Map<String, Object>> recentSessions;

    // ── Getters & Setters ─────────────────────────────────────────

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String e) {
        this.userEmail = e;
    }

    public long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(long t) {
        this.totalSessions = t;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double s) {
        this.averageScore = s;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(int s) {
        this.highestScore = s;
    }

    public String getBestRole() {
        return bestRole;
    }

    public void setBestRole(String r) {
        this.bestRole = r;
    }

    public String getWeakestRole() {
        return weakestRole;
    }

    public void setWeakestRole(String r) {
        this.weakestRole = r;
    }

    public String getLastInterviewDate() {
        return lastInterviewDate;
    }

    public void setLastInterviewDate(String d) {
        this.lastInterviewDate = d;
    }

    public Map<String, Double> getRoleWiseAverage() {
        return roleWiseAverage;
    }

    public void setRoleWiseAverage(Map<String, Double> m) {
        this.roleWiseAverage = m;
    }

    public List<String> getWeakAreas() {
        return weakAreas;
    }

    public void setWeakAreas(List<String> w) {
        this.weakAreas = w;
    }

    public List<Map<String, Object>> getRecentSessions() {
        return recentSessions;
    }

    public void setRecentSessions(List<Map<String, Object>> r) {
        this.recentSessions = r;
    }
}
