package com.example.demo.dto;

import lombok.Data;

@Data
public class AuthResponseDto {
    private String token;
    private Long userId;
    private String username;
    
    public AuthResponseDto(String token, Long userId, String username) {
        this.token = token;
        this.userId = userId;
        this.username = username;
    }
}