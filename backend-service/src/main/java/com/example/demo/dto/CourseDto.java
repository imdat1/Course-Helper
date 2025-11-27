package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private Long ownerId;
    private String ownerUsername;
    private String pdfCollectionName;
    private String docxCollectionName;
    private String pptxCollectionName;
    private VideoDto video;
    private List<FlashCardDto> flashCards;
    private List<CourseQuestionDto> questions;
}