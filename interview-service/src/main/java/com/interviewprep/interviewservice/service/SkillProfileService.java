package com.interviewprep.interviewservice.service;

import com.interviewprep.interviewservice.entity.InterviewSession;
import com.interviewprep.interviewservice.entity.UserSkillProfile;
import com.interviewprep.interviewservice.repository.AnswerRepository;
import com.interviewprep.interviewservice.repository.InterviewSessionRepository;
import com.interviewprep.interviewservice.repository.QuestionRepository;
import com.interviewprep.interviewservice.repository.UserSkillProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Day 12 – User Skill Profile Service.
 *
 * Maintains a per-user-per-role skill profile after every completed session.
 * Calculates confidence score and level from historical performance.
 */
@Service
@Transactional
public class SkillProfileService {

    private static final Logger log = LoggerFactory.getLogger(SkillProfileService.class);

    private final UserSkillProfileRepository profileRepo;
    private final InterviewSessionRepository sessionRepo;
    private final AnswerRepository answerRepo;
    private final QuestionRepository questionRepo;

    public SkillProfileService(UserSkillProfileRepository profileRepo,
            InterviewSessionRepository sessionRepo,
            AnswerRepository answerRepo,
            QuestionRepository questionRepo) {
        this.profileRepo = profileRepo;
        this.sessionRepo = sessionRepo;
        this.answerRepo = answerRepo;
        this.questionRepo = questionRepo;
    }

    /**
     * Called after every completed session to refresh the skill profile.
     */
    public UserSkillProfile updateProfile(String email, String role) {
        log.info("Updating skill profile | user={} role={}", email, role);

        List<InterviewSession> sessions = sessionRepo.findByUserEmailAndRole(email, role)
                .stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .sorted(Comparator.comparing(InterviewSession::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (sessions.isEmpty())
            return null;

        // ── Average score (percentage) ─────────────────────────
        double avgPct = sessions.stream()
                .filter(s -> s.getTotalQuestions() != null && s.getTotalQuestions() > 0)
                .mapToDouble(s -> calculatePercentageScore(s))
                .average().orElse(0.0);

        // ── Recent trend: avg of last 3 sessions ───────────────
        double recentTrend = sessions.stream()
                .filter(s -> s.getTotalQuestions() != null && s.getTotalQuestions() > 0)
                .mapToDouble(s -> calculatePercentageScore(s))
                .skip(Math.max(0L, sessions.size() - 3))
                .average().orElse(avgPct);

        // ── Confidence score ───────────────────────────────────
        double confidence = Math.min(100.0, 0.7 * avgPct + 0.3 * recentTrend);
        String level = confidence >= 70 ? "HIGH" : confidence >= 45 ? "MEDIUM" : "LOW";

        // ── Topic analysis from last 5 sessions ────────────────
        Map<String, List<Integer>> topicScores = new HashMap<>();
        sessions.stream().skip(Math.max(0, sessions.size() - 5)).forEach(s -> {
            questionRepo.findBySessionIdOrderByQuestionOrder(s.getId()).forEach(q -> {
                if (q.getTopic() != null && !q.getTopic().isBlank()) {
                    answerRepo.findBySessionIdAndQuestionId(s.getId(), q.getId())
                            .ifPresent(a -> topicScores
                                    .computeIfAbsent(q.getTopic(), k -> new ArrayList<>())
                                    .add(a.getScore() == null ? 0 : a.getScore()));
                }
            });
        });

        String weakTopics = topicScores.entrySet().stream()
                .filter(e -> avg(e.getValue()) < 50)
                .map(Map.Entry::getKey).sorted().collect(Collectors.joining(","));

        String strongTopics = topicScores.entrySet().stream()
                .filter(e -> avg(e.getValue()) >= 75)
                .map(Map.Entry::getKey).sorted().collect(Collectors.joining(","));

        // ── Upsert profile ─────────────────────────────────────
        UserSkillProfile profile = profileRepo.findByUserEmailAndRole(email, role)
                .orElse(UserSkillProfile.builder().userEmail(email).role(role).build());

        profile.setAvgScore(Math.round(avgPct * 10.0) / 10.0);
        profile.setRecentTrend(Math.round(recentTrend * 10.0) / 10.0);
        profile.setConfidenceScore(Math.round(confidence * 10.0) / 10.0);
        profile.setConfidenceLevel(level);
        profile.setWeakTopics(weakTopics);
        profile.setStrongTopics(strongTopics);
        profile.setTotalSessions(sessions.size());
        profile.setTotalCompleted(sessions.size());
        profile.setLastUpdated(LocalDateTime.now());

        UserSkillProfile saved = profileRepo.save(profile);
        log.info("Profile updated | confidence={}({}) avgScore={}", level, confidence, avgPct);
        return saved;
    }

    public Optional<UserSkillProfile> getProfile(String email, String role) {
        return profileRepo.findByUserEmailAndRole(email, role);
    }

    public List<UserSkillProfile> getAllProfiles(String email) {
        return profileRepo.findByUserEmail(email);
    }

    private double avg(List<Integer> scores) {
        return scores.stream().mapToInt(i -> i).average().orElse(0.0);
    }
    
    /**
     * Calculate the percentage score for a session based on the actual max possible score
     * considering the mix of question types (MCQ vs Subjective).
     */
    private double calculatePercentageScore(InterviewSession session) {
        // If we have the actual questions for this session, calculate based on max possible score
        // Otherwise, fall back to the simple calculation
        try {
            List<com.interviewprep.interviewservice.entity.Question> questions = 
                questionRepo.findBySessionIdOrderByQuestionOrder(session.getId());
            
            if (questions.isEmpty()) {
                // Fallback to original calculation if we can't get questions
                return session.getTotalQuestions() > 0 ? 
                    (session.getScore() * 100.0) / session.getTotalQuestions() : 0.0;
            }
            
            // Calculate max possible score based on question types
            int maxPossibleScore = 0;
            for (com.interviewprep.interviewservice.entity.Question q : questions) {
                if ("mcq".equalsIgnoreCase(q.getType())) {
                    maxPossibleScore += 1; // MCQ: max 1 point
                } else if ("subjective".equalsIgnoreCase(q.getType())) {
                    maxPossibleScore += 10; // Subjective: max 10 points
                } else {
                    // Default to 10 for unknown types to be safe (assuming subjective)
                    maxPossibleScore += 10;
                }
            }
            
            // Calculate percentage based on actual max possible score
            return maxPossibleScore > 0 ? 
                (session.getScore() * 100.0) / maxPossibleScore : 0.0;
                
        } catch (Exception e) {
            // Fallback to original calculation if there's an error
            return session.getTotalQuestions() > 0 ? 
                (session.getScore() * 100.0) / session.getTotalQuestions() : 0.0;
        }
    }
}
