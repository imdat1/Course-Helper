package com.example.demo.dto;

public class AnswerEvaluationRequestDto {
    private String question;
    private String expectedAnswer;
    private String userAnswer;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getExpectedAnswer() { return expectedAnswer; }
    public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
}
