package com.interviewprep.interviewservice.service;

import com.interviewprep.interviewservice.entity.Answer;
import com.interviewprep.interviewservice.entity.Question;
import com.interviewprep.interviewservice.repository.AnswerRepository;
import com.interviewprep.interviewservice.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Day 9 – Weak Topic Detection Service.
 *
 * Tracks performance per topic across a session.
 * Identifies topics below 50% average → flagged as weak.
 *
 * Example output:
 * { "OOP": 40.0, "Collections": 90.0, "Threading": 30.0 }
 * Weak topics → ["OOP", "Threading"]
 */
@Service
public class WeakTopicService {

    private static final Logger log = LoggerFactory.getLogger(WeakTopicService.class);

    /** Percentage threshold below which a topic is considered weak. */
    private static final double WEAK_THRESHOLD = 50.0;

    private final QuestionRepository questionRepo;
    private final AnswerRepository answerRepo;

    public WeakTopicService(QuestionRepository questionRepo, AnswerRepository answerRepo) {
        this.questionRepo = questionRepo;
        this.answerRepo = answerRepo;
    }

    /**
     * Calculate topic-wise score percentages for a completed session.
     *
     * @param sessionId Completed session ID
     * @return Map of topic → percentage score
     */
    public Map<String, Double> getTopicScores(Long sessionId) {
        List<Question> questions = questionRepo.findBySessionIdOrderByQuestionOrder(sessionId);
        List<Answer> answers = answerRepo.findBySessionId(sessionId);

        // Build answer lookup: questionId → score
        Map<Long, Integer> answerScores = answers.stream()
                .collect(Collectors.toMap(
                        Answer::getQuestionId,
                        a -> a.getScore() == null ? 0 : a.getScore(),
                        (a, b) -> a // keep first if duplicate
                ));

        // Group by topic: topic → [scores]
        Map<String, List<Integer>> topicScoreMap = new LinkedHashMap<>();
        for (Question q : questions) {
            String topic = (q.getTopic() != null && !q.getTopic().isBlank())
                    ? q.getTopic()
                    : "General";
            int score = answerScores.getOrDefault(q.getId(), 0);
            topicScoreMap.computeIfAbsent(topic, k -> new ArrayList<>()).add(score);
        }

        // Convert to average %
        Map<String, Double> topicAvg = new LinkedHashMap<>();
        topicScoreMap.forEach((topic, scores) -> {
            double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
            topicAvg.put(topic, Math.round(avg * 100.0) / 100.0);
        });

        log.info("Topic scores for session {}: {}", sessionId, topicAvg);
        return topicAvg;
    }

    /**
     * Returns list of weak topics (avg < 50%).
     *
     * @param sessionId Session to analyse
     * @return Sorted list of weak topic names
     */
    public List<String> getWeakTopics(Long sessionId) {
        return getTopicScores(sessionId).entrySet().stream()
                .filter(e -> e.getValue() < WEAK_THRESHOLD)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    /**
     * Returns the single most critical weak topic (lowest score).
     * Used to personalise the NEXT session's question generation.
     *
     * @param sessionId Session to analyse
     * @return Weakest topic name, or null if no weak topics
     */
    public String getPrimaryWeakTopic(Long sessionId) {
        return getTopicScores(sessionId).entrySet().stream()
                .filter(e -> e.getValue() < WEAK_THRESHOLD)
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
