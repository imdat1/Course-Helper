package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoUriResponseDto {
    private VideoDto video;
    
    @JsonProperty("uri_provider")
    private String uriProvider;
    
    @JsonProperty("api_key")
    private String apiKey;
    
    @JsonProperty("task_id")
    private String taskId;
    
    private String status;
}