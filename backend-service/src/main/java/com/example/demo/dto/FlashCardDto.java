package com.example.demo.dto;

import lombok.Data;

@Data
public class FlashCardDto {
    private Long id;
    private String question;
    private String answer;
}