package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessAiResponseDto {
    @JsonProperty("task_id")
    private String taskId;
    
    private String status;
    
    @JsonProperty("conversation_history")
    private ConversationHistoryDto conversationHistory;
}