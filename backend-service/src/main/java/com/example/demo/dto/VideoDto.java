package com.example.demo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class VideoDto {
    private String type;
    private String uri;
    private String path;
    private Map<String, String> uriData;
    private DurationDto duration;
    
    @Data
    public static class DurationDto {
        private Integer minutes;
        private Integer seconds;
    }
}