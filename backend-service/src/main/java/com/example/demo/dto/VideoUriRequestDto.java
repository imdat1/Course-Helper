package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class VideoUriRequestDto {
    private VideoDto video;
    
    @JsonProperty("uri_provider")
    private String uriProvider;
    
    @JsonProperty("api_key")
    private String apiKey;
}