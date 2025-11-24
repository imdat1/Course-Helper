package com.example.demo.dto;

import lombok.Data;

@Data
public class FlashCardRequestDto {
    private String collectionName;  // For PDF/DOCX 
    private VideoDto video;         // For video
}