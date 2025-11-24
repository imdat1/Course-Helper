package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class FlashCardResponseDto {
    private List<FlashCardDto> flashCards;
}