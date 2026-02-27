package com.interviewprep.interviewservice.dto;

/**
 * Request DTO for subjective answer evaluation.
 *
 * Example:
 * {
 *   "question": "Explain Spring Boot auto-configuration.",
 *   "answer": "Spring Boot auto-configuration works by..."
 * }
 */
public class EvaluateSubjectiveRequest {

    private String question;
    private String answer;

    public String getQuestion()          { return question; }
    public void setQuestion(String q)    { this.question = q; }

    public String getAnswer()            { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    @Override
    public String toString() {
        return "EvaluateSubjectiveRequest{question='" + question + "'}";
    }
}
