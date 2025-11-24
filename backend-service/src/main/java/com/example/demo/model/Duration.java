package com.example.demo.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Duration {
    private Integer minutes;
    private Integer seconds;

    // Constructors
    public Duration() {}

    public Duration(Integer minutes, Integer seconds) {
        this.minutes = minutes;
        this.seconds = seconds;
    }

    // Getters and Setters
    public Integer getMinutes() {
        return minutes;
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }

    public Integer getSeconds() {
        return seconds;
    }

    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }
}