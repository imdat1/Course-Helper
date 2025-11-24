// Never Used This File
package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfig {
    @Value("${api.key}")
    private String apiKey;
    
    @Value("${api.base-url}")
    private String baseUrl;
    
    @Value("${api.model-provider:gemini}")
    private String modelProvider;
    
    @Value("${api.model-name:gemini-2.0-flash}")
    private String modelName;
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public String getModelProvider() {
        return modelProvider;
    }
    
    public String getModelName() {
        return modelName;
    }
}