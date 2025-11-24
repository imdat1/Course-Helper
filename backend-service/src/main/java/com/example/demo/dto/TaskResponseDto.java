package com.example.demo.dto;

import lombok.Data;

@Data
public class TaskResponseDto {
    private String taskId;
    private String status;  // "PENDING", "SUCCESS", "FAILURE"
    private Object result;  // Generic result that can be mapped to specific DTOs
}