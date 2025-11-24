package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ConversationHistoryDto {
    private List<MessageDto> messages;
}