package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDto {
    private Long id;
    private String role;
    private String content;
    private LocalDateTime timestamp;
    private Long courseId;
}