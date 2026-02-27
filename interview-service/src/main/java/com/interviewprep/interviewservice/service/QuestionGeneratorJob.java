package com.interviewprep.interviewservice.service;

import com.interviewprep.interviewservice.entity.QuestionBank;
import com.interviewprep.interviewservice.repository.QuestionBankRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Day 13 – Background Question Generator Job.
 *
 * Runs hourly to pre-generate questions for popular roles and store them
 * in QuestionBank. InterviewService uses these for fast session starts.
 */
@Service
public class QuestionGeneratorJob {

    private static final Logger log = LoggerFactory.getLogger(QuestionGeneratorJob.class);

    private static final List<String> POPULAR_ROLES = List.of("Java Developer", "Python Developer", "Data Analyst",
            "Frontend Developer", "Backend Developer", "DevOps Engineer");

    private static final List<String> LEVELS = List.of("easy", "medium", "hard");

    /** Minimum questions per role+level combo before regenerating. */
    private static final int MIN_BANK_SIZE = 10;

    private final QuestionBankRepository bankRepo;
    private final AIClientService aiClientService;

    public QuestionGeneratorJob(QuestionBankRepository bankRepo,
            AIClientService aiClientService) {
        this.bankRepo = bankRepo;
        this.aiClientService = aiClientService;
    }

    /**
     * Runs every hour. Generates subjective + MCQ questions for popular roles
     * where the bank is below minimum threshold.
     */
    @Scheduled(fixedRate = 3_600_000) // 1 hour
    public void generateDailyQuestions() {
        log.info("[QuestionGeneratorJob] Starting hourly question pre-generation...");
        int totalGenerated = 0;

        for (String role : POPULAR_ROLES) {
            for (String level : LEVELS) {
                long count = bankRepo.countByRoleAndLevel(role, level);
                if (count >= MIN_BANK_SIZE) {
                    log.debug("Bank sufficient for role={} level={} ({})", role, level, count);
                    continue;
                }

                try {
                    totalGenerated += generateSubjectiveForBank(role, level);
                    totalGenerated += generateMcqForBank(role, level);
                } catch (Exception e) {
                    log.warn("Failed to generate for role={} level={}: {}", role, level, e.getMessage());
                }
            }
        }

        log.info("[QuestionGeneratorJob] Done. Total questions generated: {}", totalGenerated);
    }

    private int generateSubjectiveForBank(String role, String level) {
        String rawJson = aiClientService.generateSubjective(role, level, null);
        String[] parts = rawJson.split("\"question\"\\s*:\\s*\"");
        int count = 0;
        for (int i = 1; i < parts.length; i++) {
            int end = parts[i].indexOf("\"");
            if (end > 0) {
                String text = parts[i].substring(0, end).replace("\\n", "\n").trim();
                String topic = extractTopic(parts[i]);
                bankRepo.save(QuestionBank.builder()
                        .role(role).level(level).type("subjective")
                        .questionText(text).topic(topic).build());
                count++;
            }
        }
        return count;
    }

    private int generateMcqForBank(String role, String level) {
        String rawJson = aiClientService.generateMCQ(role, level, null);
        String[] parts = rawJson.split("\"question\"\\s*:\\s*\"");
        int count = 0;
        for (int i = 1; i < parts.length; i++) {
            int end = parts[i].indexOf("\"");
            if (end > 0) {
                String text = parts[i].substring(0, end).replace("\\n", "\n").trim();
                String correct = extractField(parts[i], "correctAnswer");
                String topic = extractTopic(parts[i]);
                bankRepo.save(QuestionBank.builder()
                        .role(role).level(level).type("mcq")
                        .questionText(text).correctAnswer(correct).topic(topic).build());
                count++;
            }
        }
        return count;
    }

    private String extractField(String block, String field) {
        int idx = block.indexOf("\"" + field + "\"");
        if (idx < 0)
            return "";
        int start = block.indexOf("\"", idx + field.length() + 3) + 1;
        int end = block.indexOf("\"", start);
        return (start > 0 && end > start) ? block.substring(start, end).trim() : "";
    }

    private String extractTopic(String block) {
        String t = extractField(block, "topic");
        return t.isBlank() ? "General" : t;
    }
}
