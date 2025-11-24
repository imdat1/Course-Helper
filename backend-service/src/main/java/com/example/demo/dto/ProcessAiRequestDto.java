package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ProcessAiRequestDto {
    @JsonProperty("model_provider")
    private String modelProvider;
    
    @JsonProperty("model_name")
    private String modelName;
    
    @JsonProperty("system_instruction_text")
    private String systemInstructionText;
    
    @JsonProperty("conversation_history")
    private ConversationHistoryDto conversationHistory;
    
    @JsonProperty("pdf_collection_name")
    private String pdfCollectionName;
    
    @JsonProperty("docx_collection_name")
    private String docxCollectionName;
    
    private VideoDto video;
    
    @JsonProperty("api_key")
    private String apiKey;
}