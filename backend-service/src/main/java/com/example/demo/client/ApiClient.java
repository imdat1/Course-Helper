package com.example.demo.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.demo.dto.FlashCardRequestDto;
import com.example.demo.dto.ProcessAiRequestDto;
import com.example.demo.dto.ProcessAiResponseDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.UploadResponseDto;
import com.example.demo.dto.VideoUriRequestDto;
import com.example.demo.dto.VideoUriResponseDto;
import com.example.demo.dto.AnswerEvaluationRequestDto;
import com.example.demo.dto.AnswerEvaluationResponseDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import reactor.core.publisher.Mono;
import org.springframework.core.ParameterizedTypeReference;

@Component
public class ApiClient {
    private final WebClient webClient;
    private final String apiKey;
    private final String modelName;
    private final String modelProvider;
    
    public ApiClient(
            @Value("${api.base-url}") String baseUrl,
            @Value("${api.key}") String apiKey,
            @Value("${api.model-name}") String modelName,
            @Value("${api.model-provider}") String modelProvider) {
        
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.modelProvider = modelProvider;
    
        // Create ObjectMapper with snake_case property naming
        ObjectMapper objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Configure exchange strategies with higher memory limit
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                        configurer.defaultCodecs().jackson2JsonEncoder(
                                new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                        configurer.defaultCodecs().jackson2JsonDecoder(
                                new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB buffer
                })
                .build();
        
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(exchangeStrategies)
                .build();
                }
    
    public Mono<TaskResponseDto> getTaskStatus(String taskId) {
        return webClient.get()
                .uri("/api/task-status/{taskId}", taskId)
                .retrieve()
                .bodyToMono(TaskResponseDto.class);
    }

        public Mono<AnswerEvaluationResponseDto> evaluateFlashCardAnswer(
                        AnswerEvaluationRequestDto request, String collectionName) {
                Map<String, Object> body = new HashMap<>();
                body.put("question", request.getQuestion());
                body.put("expected_answer", request.getExpectedAnswer());
                body.put("user_answer", request.getUserAnswer());
                body.put("api_key", apiKey);
                if (collectionName != null && !collectionName.isEmpty()) {
                        body.put("collection_name", collectionName);
                }

                return webClient.post()
                                .uri("/api/evaluate_flash_card_answer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(body)
                                .retrieve()
                                .bodyToMono(AnswerEvaluationResponseDto.class);
        }
    
        public Mono<UploadResponseDto> uploadPdfFile(MultipartFile file, String collectionName) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file.getResource());
                if (collectionName != null) {
                        parts.add("collection_name", collectionName);
                }
        
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/upload/")
                        .queryParam("api_key", apiKey)
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts)
                .retrieve()
                .bodyToMono(UploadResponseDto.class);
    }
    
        public Mono<UploadResponseDto> uploadDocxFile(MultipartFile file, String collectionName) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file.getResource());
                if (collectionName != null) {
                        parts.add("collection_name", collectionName);
                }
        
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/upload/")
                        .queryParam("api_key", apiKey)
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts)
                .retrieve()
                .bodyToMono(UploadResponseDto.class);
    }
    
        public Mono<UploadResponseDto> uploadVideo(MultipartFile file) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file.getResource());
        
        return webClient.post()
                .uri("/api/upload/")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts)
                .retrieve()
                .bodyToMono(UploadResponseDto.class);
    }

    public Mono<UploadResponseDto> uploadXmlFile(MultipartFile file, String collectionName) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file.getResource());
        if (collectionName != null) {
            parts.add("collection_name", collectionName);
        }
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/upload/")
                        .queryParam("api_key", apiKey)
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts)
                .retrieve()
                .bodyToMono(UploadResponseDto.class);
    }

                        public Mono<Map<String, Object>> deleteFileFromCollection(String collectionName, String fileId) {
                MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
                parts.add("collection_name", collectionName);
                parts.add("file_id", fileId);
                return webClient.post()
                                .uri("/api/delete_file_from_collection")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .bodyValue(parts)
                                .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
        }

        public Mono<Map<String, Object>> deleteCollection(String collectionName) {
                MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
                parts.add("collection_name", collectionName);
                return webClient.post()
                                .uri("/api/delete_collection")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .bodyValue(parts)
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
        }
    
    public Mono<VideoUriResponseDto> getVideoUri(VideoUriRequestDto request) {
        // Create a map for the request body
        Map<String, Object> requestMap = new HashMap<>();
        
        // Add video object
        Map<String, Object> videoMap = new HashMap<>();
        videoMap.put("type", request.getVideo().getType());
        videoMap.put("path", request.getVideo().getPath());
        requestMap.put("video", videoMap);
        
        // Add uri provider
        requestMap.put("uri_provider", request.getUriProvider());
        
        // Add API key
        requestMap.put("api_key", apiKey);
        
        System.out.println("Sending getVideoUri request with: " + requestMap);
        
        return webClient.post()
                .uri("/api/get_video_uri")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestMap)
                .retrieve()
                .bodyToMono(VideoUriResponseDto.class)
                .doOnNext(response -> System.out.println("Received getVideoUri response: " + response))
                .doOnError(error -> System.err.println("Error in getVideoUri: " + error.getMessage()));
        }
    
    public Mono<ProcessAiResponseDto> processAi(ProcessAiRequestDto request) {
        // Create a map for the request body
        Map<String, Object> requestMap = new HashMap<>();
        
        // Add API key to the request body
        requestMap.put("api_key", apiKey);
        
        // Add the required 'type' field
        requestMap.put("type", "PROCESS_AI");
        
        // Add all the fields from the request
        requestMap.put("model_provider", request.getModelProvider());
        requestMap.put("model_name", request.getModelName());
        requestMap.put("system_instruction_text", request.getSystemInstructionText());
        
        // Add conversation history with role mapping
        if (request.getConversationHistory() != null && request.getConversationHistory().getMessages() != null) {
                Map<String, Object> conversationMap = new HashMap<>();
                
                List<Map<String, String>> messagesMapList = request.getConversationHistory().getMessages().stream()
                        .map(msg -> {
                        Map<String, String> messageMap = new HashMap<>();
                        
                        // Map "assistant" role to "model" as required by the API
                        String role = msg.getRole();
                        if ("assistant".equals(role)) {
                                role = "model";
                        }
                        
                        messageMap.put("role", role);
                        messageMap.put("content", msg.getContent());
                        return messageMap;
                        })
                        .collect(Collectors.toList());
                
                conversationMap.put("messages", messagesMapList);
                requestMap.put("conversation_history", conversationMap);
        }
        
        // Rest of your existing code...
        // Add PDF or DOCX collection name if available
        if (request.getPdfCollectionName() != null) {
                requestMap.put("pdf_collection_name", request.getPdfCollectionName());
        }
        if (request.getDocxCollectionName() != null) {
                requestMap.put("docx_collection_name", request.getDocxCollectionName());
        }
        
        // Add video if available
        if (request.getVideo() != null) {
                Map<String, Object> videoMap = new HashMap<>();
                videoMap.put("type", request.getVideo().getType());
                
                if (request.getVideo().getUri() != null) {
                videoMap.put("uri", request.getVideo().getUri());
                }
                if (request.getVideo().getPath() != null) {
                videoMap.put("path", request.getVideo().getPath());
                }
                
                // Add duration if present
                if (request.getVideo().getDuration() != null) {
                Map<String, Integer> durationMap = new HashMap<>();
                durationMap.put("minutes", request.getVideo().getDuration().getMinutes());
                durationMap.put("seconds", request.getVideo().getDuration().getSeconds());
                videoMap.put("duration", durationMap);
                }
                
                requestMap.put("video", videoMap);
        }
        
        System.out.println("Sending process-ai request: " + requestMap);
        
        return webClient.post()
                .uri("/api/process-ai")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestMap)
                .retrieve()
                .bodyToMono(ProcessAiResponseDto.class)
                .doOnNext(response -> System.out.println("Received process-ai response: " + response))
                .doOnError(error -> {
                        System.err.println("Error in processAi: " + error.getMessage());
                        if (error instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) error;
                        System.err.println("Response body: " + wcre.getResponseBodyAsString());
                        }
                });
        }
    
    public Mono<TaskResponseDto> createFlashCards(FlashCardRequestDto request) {
        // Create a map for the request body
        Map<String, Object> requestMap = new HashMap<>();
        
        // Set the type in the request body (not the video object)
        requestMap.put("type", request.getVideo() != null ? request.getVideo().getType() : "FILE_VIDEO");
        
        // Default number of flash cards to generate
        requestMap.put("count", 5);
        
        // Handle PDF or DOCX collection name
        if (request.getCollectionName() != null && !request.getCollectionName().isEmpty()) {
                requestMap.put("collection_name", request.getCollectionName());
                System.out.println("Adding collection_name: " + request.getCollectionName());
        }
        
        // For YouTube videos, we need the URI
        if (request.getVideo() != null) {
                if ("YOUTUBE_VIDEO".equals(request.getVideo().getType())) {
                if (request.getVideo().getUri() != null) {
                        requestMap.put("uri", request.getVideo().getUri());
                        System.out.println("Adding YouTube URI: " + request.getVideo().getUri());
                } else {
                        System.err.println("YouTube video is missing URI!");
                        return Mono.error(new RuntimeException("YouTube video is missing URI"));
                }
                }
                
                // For file videos, we need the URI
                if ("FILE_VIDEO".equals(request.getVideo().getType())) {
                if (request.getVideo().getUri() != null) {
                        requestMap.put("uri", request.getVideo().getUri());
                        System.out.println("Adding File Video URI: " + request.getVideo().getUri());
                } else if (request.getVideo().getPath() != null) {
                        requestMap.put("path", request.getVideo().getPath());
                        System.out.println("Adding File Video path: " + request.getVideo().getPath());
                } else {
                        System.err.println("File video is missing both URI and path!");
                        return Mono.error(new RuntimeException("File video is missing both URI and path"));
                }
                }
                
                // Add duration if present
                if (request.getVideo().getDuration() != null) {
                Map<String, Integer> durationMap = new HashMap<>();
                durationMap.put("minutes", request.getVideo().getDuration().getMinutes());
                durationMap.put("seconds", request.getVideo().getDuration().getSeconds());
                requestMap.put("duration", durationMap);
                }
        }
        
        System.out.println("Sending flash card request: " + requestMap);
        
        // API key is sent as a query parameter, not in the body
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/create_flash_cards")
                        .queryParam("api_key", apiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestMap)
                .retrieve()
                .bodyToMono(TaskResponseDto.class)
                .doOnNext(response -> System.out.println("Received flash card response: " + response))
                .doOnError(error -> {
                        System.err.println("Error in createFlashCards: " + error.getMessage());
                        if (error instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) error;
                        System.err.println("Response body: " + wcre.getResponseBodyAsString());
                        }
                });
        }

    
}