package com.interviewprep.interviewservice.dto;

import java.util.List;

/**
 * Request DTO for MCQ evaluation.
 *
 * Example:
 * {
 *   "questions": [
 *       {
 *           "question": "What is JVM?",
 *           "options": ["A) ...", "B) ...", "C) ...", "D) ..."],
 *           "correctAnswer": "A",
 *           "selectedAnswer": "A"
 *       }
 *   ]
 * }
 */
public class MCQEvaluateRequest {

    private List<MCQAnswer> questions;

    public List<MCQAnswer> getQuestions() { return questions; }
    public void setQuestions(List<MCQAnswer> questions) { this.questions = questions; }

    // ─── Inner class representing one answered MCQ ───────────────
    public static class MCQAnswer {
        private String question;
        private List<String> options;
        private String correctAnswer;
        private String selectedAnswer;

        public String getQuestion()       { return question; }
        public void setQuestion(String q) { this.question = q; }

        public List<String> getOptions()          { return options; }
        public void setOptions(List<String> opts) { this.options = opts; }

        public String getCorrectAnswer()         { return correctAnswer; }
        public void setCorrectAnswer(String ans) { this.correctAnswer = ans; }

        public String getSelectedAnswer()         { return selectedAnswer; }
        public void setSelectedAnswer(String ans) { this.selectedAnswer = ans; }
    }
}
