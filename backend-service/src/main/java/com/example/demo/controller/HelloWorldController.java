package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.model.HelloResponse;

@RestController
public class HelloWorldController {

    @GetMapping("/hello")
    public HelloResponse getHello() {
        return new HelloResponse("Hello World!");
    }
}