package com.interviewprep.interviewservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for submitting an answer during an interview session.
 *
 * {
 * "sessionId": 1,
 * "questionId": 5,
 * "answer": "My answer here"
 * }
 */
public class AnswerDTO {

    @NotNull
    private Long sessionId;

    @NotNull
    private Long questionId;

    @NotBlank
    private String answer;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long id) {
        this.sessionId = id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long id) {
        this.questionId = id;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
