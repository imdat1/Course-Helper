package com.example.demo.dto;

import lombok.Data;

@Data
public class CourseQuestionDto {
    private String id;
    private String fileId;
    private String questionText; // raw HTML
    private String answersJson; // JSON array of parsed answers (includes ai_reasoning per entry)
    private String imagesJson; // JSON array of base64 images or metadata
}