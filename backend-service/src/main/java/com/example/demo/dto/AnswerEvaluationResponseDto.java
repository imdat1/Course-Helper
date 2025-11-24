package com.example.demo.dto;

import java.util.Map;

public class AnswerEvaluationResponseDto {
    private String question;
    private String expectedAnswer;
    private String userAnswer;
    private Map<String, Object> evaluation;
    private String sourceFileName;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public Map<String, Object> getEvaluation() { return evaluation; }
    public void setEvaluation(Map<String, Object> evaluation) { this.evaluation = evaluation; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
}
