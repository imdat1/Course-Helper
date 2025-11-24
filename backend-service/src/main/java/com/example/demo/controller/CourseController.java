package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.client.ApiClient;
import com.example.demo.dto.ConversationHistoryDto;
import com.example.demo.dto.CourseDto;
import com.example.demo.dto.CourseQuestionDto;
import com.example.demo.dto.FlashCardDto;
import com.example.demo.dto.FlashCardRequestDto;
import com.example.demo.dto.AnswerEvaluationRequestDto;
import com.example.demo.dto.AnswerEvaluationResponseDto;
import com.example.demo.dto.MessageDto;
import com.example.demo.dto.ProcessAiRequestDto;
import com.example.demo.dto.ProcessAiResponseDto;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.UploadResponseDto;
import com.example.demo.dto.VideoDto;
import com.example.demo.dto.VideoUriRequestDto;
import com.example.demo.dto.VideoUriResponseDto;
import com.example.demo.model.Course;
import com.example.demo.model.CourseQuestion;
import com.example.demo.model.Duration;
import com.example.demo.model.FlashCard;
import com.example.demo.model.Message;
import com.example.demo.model.Task;
import com.example.demo.model.Video;
import com.example.demo.repository.FlashCardRepository;
import com.example.demo.service.CourseQuestionService;
import com.example.demo.service.CourseService;
import com.example.demo.service.MessageService;
import com.example.demo.service.TaskService;
import com.example.demo.service.UploadedFileService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final UserService userService;
    private final ApiClient apiClient;
    private final TaskService taskService;
    private final MessageService messageService;
    private final UploadedFileService uploadedFileService;
    private final FlashCardRepository flashCardRepository;
    private final CourseQuestionService courseQuestionService;
    
    @Autowired
    public CourseController(CourseService courseService, UserService userService, 
                           ApiClient apiClient, TaskService taskService, MessageService messageService, UploadedFileService uploadedFileService, FlashCardRepository flashCardRepository, CourseQuestionService courseQuestionService) {
        this.courseService = courseService;
        this.userService = userService;
        this.apiClient = apiClient;
        this.taskService = taskService;
        this.messageService = messageService;
        this.uploadedFileService = uploadedFileService;
        this.flashCardRepository = flashCardRepository;
        this.courseQuestionService = courseQuestionService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CourseDto> getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CourseDto>> getCoursesByUser(@PathVariable Long userId) {
        return userService.getUserById(userId)
                .map(user -> {
                    List<CourseDto> courses = courseService.getCoursesByOwner(user).stream()
                            .map(this::convertToDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(courses);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<CourseDto> createCourse(@RequestBody CourseDto courseDto) {
        return userService.getUserById(courseDto.getOwnerId())
                .map(user -> {
                    Course course = new Course(courseDto.getTitle(), courseDto.getDescription());
                    course.setOwner(user);
                    Course savedCourse = courseService.createCourse(course);
                    return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedCourse));
                })
                .orElse(ResponseEntity.badRequest().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CourseDto> updateCourse(@PathVariable Long id, @RequestBody CourseDto courseDto) {
        return courseService.getCourseById(id)
                .map(course -> {
                    course.setTitle(courseDto.getTitle());
                    course.setDescription(courseDto.getDescription());
                    Course updatedCourse = courseService.updateCourse(course);
                    return ResponseEntity.ok(convertToDto(updatedCourse));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        if (!courseService.getCourseById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/upload-pdf")
    public ResponseEntity<CourseDto> uploadPdf(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        // Verify course exists
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Ensure a stable shared collection for ALL text materials (PDF + DOCX) for this course
        String materialsCollection = course.getPdfCollectionName();
        if (materialsCollection == null || materialsCollection.isEmpty()) {
            materialsCollection = course.getDocxCollectionName();
        }
        if (materialsCollection == null || materialsCollection.isEmpty()) {
            materialsCollection = "course_" + id + "_materials";
            // Set both PDF and DOCX collection names to the same shared value
            courseService.setPdfCollectionName(id, materialsCollection);
            courseService.setDocxCollectionName(id, materialsCollection);
            course = courseService.getCourseById(id).orElse(course);
        }

    // Upload file and get initial response (pass the shared collection)
    UploadResponseDto response = apiClient.uploadPdfFile(file, materialsCollection).block();
        if (response == null || response.getTaskId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload PDF");
        }
        
        // Create task for tracking (still useful for background processing)
    // Track initial upload task (ignored variable)
    taskService.createTask(response.getTaskId(), "PENDING", id);

        // Early persist UploadedFile metadata with PENDING status
        try {
            com.example.demo.model.UploadedFile uf = new com.example.demo.model.UploadedFile();
            uf.setCourse(course);
            uf.setType("PDF");
            uf.setFilename(response.getFilename());
            uf.setFileId(response.getFileId());
            uf.setCollectionName(materialsCollection);
            // For PDFs, the long-running step is parse/summarise/ingest; prefer that task id
            String processingTaskId = response.getParseSummariseIngestTaskId() != null ? response.getParseSummariseIngestTaskId() : response.getTaskId();
            uf.setProcessingTaskId(processingTaskId);
            uploadedFileService.save(uf);
        } catch (Exception e) {
            System.err.println("Failed early save of PDF metadata: " + e.getMessage());
        }
        
        // Check for parse task and wait for it to complete
        if (response.getParseSummariseIngestTaskId() != null) {
            String parseTaskId = response.getParseSummariseIngestTaskId();
            taskService.createTask(parseTaskId, "PENDING", id); // record parse task
            
            // Poll until the parse task completes (with timeout)
            TaskResponseDto parseTaskResponse = waitForTaskCompletion(parseTaskId);
            
            if (parseTaskResponse != null && "SUCCESS".equals(parseTaskResponse.getStatus())) {
                // Extract collection name and flash cards
                Map<String, Object> resultMap = (Map<String, Object>) parseTaskResponse.getResult();
                
                // Update per-file metadata to READY; collection is already set to shared name
                String collectionName = (String) resultMap.get("qdrant_collection_name");
                try {
                    uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                        if (existing.getCollectionName() == null && collectionName != null) {
                            existing.setCollectionName(collectionName);
                        }
                        existing.setStatus("READY");
                        uploadedFileService.save(existing);
                    });
                } catch (Exception e) {
                    System.err.println("Failed to update PDF metadata to READY: " + e.getMessage());
                }
                
                // Extract and add flash cards
                List<Map<String, String>> flashCardsMap = (List<Map<String, String>>) resultMap.get("flash_cards");
                if (flashCardsMap != null && !flashCardsMap.isEmpty()) {
                    addFlashCardsToCourseWithSource(id, flashCardsMap, response.getFileId());
                }
            } else {
                // Mark FAILED if parse task failed
                uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                    existing.setStatus("FAILED");
                    uploadedFileService.save(existing);
                });
            }
        }
        
        // Return the updated course
        return ResponseEntity.ok(convertToDto(courseService.getCourseById(id).get()));
    }

    // XML uploads are handled by Python /api/upload/; frontend can call /upload-pdf style endpoint added separately if needed.
    @PostMapping("/{id}/upload-xml")
    public ResponseEntity<CourseDto> uploadXml(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Shared collection across course materials
        String materialsCollection = course.getPdfCollectionName();
        if (materialsCollection == null || materialsCollection.isEmpty()) {
            materialsCollection = course.getDocxCollectionName();
        }
        if (materialsCollection == null || materialsCollection.isEmpty()) {
            materialsCollection = "course_" + id + "_materials";
            courseService.setPdfCollectionName(id, materialsCollection);
            courseService.setDocxCollectionName(id, materialsCollection);
            course = courseService.getCourseById(id).orElse(course);
        }

        UploadResponseDto response = apiClient.uploadXmlFile(file, materialsCollection).block();
        if (response == null || response.getProcessXmlTaskId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to enqueue XML processing");
        }

        taskService.createTask(response.getProcessXmlTaskId(), "PENDING", id);

        try {
            com.example.demo.model.UploadedFile uf = new com.example.demo.model.UploadedFile();
            uf.setCourse(course);
            uf.setType("XML");
            uf.setFilename(response.getFilename());
            uf.setFileId(response.getFileId());
            uf.setCollectionName(materialsCollection);
            uf.setProcessingTaskId(response.getProcessXmlTaskId());
            uploadedFileService.save(uf);
        } catch (Exception e) {
            System.err.println("Failed early save of XML metadata: " + e.getMessage());
        }

        TaskResponseDto xmlTaskStatus = waitForTaskCompletion(response.getProcessXmlTaskId());
        if (xmlTaskStatus != null && "SUCCESS".equals(xmlTaskStatus.getStatus())) {
            Map<String, Object> resultMap = (Map<String, Object>) xmlTaskStatus.getResult();
            Object questionsObj = resultMap.get("questions");
            if (questionsObj instanceof List) {
                List<Map<String, Object>> questionList = (List<Map<String, Object>>) questionsObj;
                for (Map<String, Object> qMap : questionList) {
                    try {
                        CourseQuestion cq = new CourseQuestion();
                        cq.setCourse(course);
                        cq.setFileId(response.getFileId());
                        cq.setQuestionText((String) qMap.get("question_text"));
                        Object parsed = qMap.get("questions_parsed_and_answered");
                        String answersJson = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(parsed);
                        cq.setAnswersJson(answersJson);
                        // Extract images array if present
                        Object imagesObj = qMap.get("question_images");
                        if (imagesObj != null) {
                            String imagesJson = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(imagesObj);
                            cq.setImagesJson(imagesJson);
                        }
                        courseQuestionService.save(cq);
                    } catch (Exception e) {
                        System.err.println("Failed to persist course question: " + e.getMessage());
                    }
                }
            }
            uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                existing.setStatus("READY");
                uploadedFileService.save(existing);
            });
        } else {
            uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                existing.setStatus("FAILED");
                uploadedFileService.save(existing);
            });
        }

        return ResponseEntity.ok(convertToDto(courseService.getCourseById(id).get()));
    }

    @PostMapping("/{id}/upload-docx")
    public ResponseEntity<CourseDto> uploadDocx(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        // Verify course exists
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Ensure the same shared collection for ALL text materials (PDF + DOCX)
        String materialsCollection2 = course.getPdfCollectionName();
        if (materialsCollection2 == null || materialsCollection2.isEmpty()) {
            materialsCollection2 = course.getDocxCollectionName();
        }
        if (materialsCollection2 == null || materialsCollection2.isEmpty()) {
            materialsCollection2 = "course_" + id + "_materials";
            courseService.setPdfCollectionName(id, materialsCollection2);
            courseService.setDocxCollectionName(id, materialsCollection2);
            course = courseService.getCourseById(id).orElse(course);
        }

    // Upload file and get initial response (pass the shared collection)
    UploadResponseDto response = apiClient.uploadDocxFile(file, materialsCollection2).block();
        if (response == null || response.getTaskId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload DOCX");
        }
        
        // Create task for tracking (still useful for background processing)
    taskService.createTask(response.getTaskId(), "PENDING", id); // track video upload task

        // Early persist UploadedFile metadata with PENDING status
        try {
            com.example.demo.model.UploadedFile uf = new com.example.demo.model.UploadedFile();
            uf.setCourse(course);
            uf.setType("DOCX");
            uf.setFilename(response.getFilename());
            uf.setFileId(response.getFileId());
            uf.setCollectionName(materialsCollection2);
            String processingTaskId = response.getParseSummariseIngestTaskId() != null ? response.getParseSummariseIngestTaskId() : response.getTaskId();
            uf.setProcessingTaskId(processingTaskId);
            uploadedFileService.save(uf);
        } catch (Exception e) {
            System.err.println("Failed early save of DOCX metadata: " + e.getMessage());
        }
        
        // Check for parse task and wait for it to complete
        if (response.getParseSummariseIngestTaskId() != null) {
            String parseTaskId = response.getParseSummariseIngestTaskId();
            taskService.createTask(parseTaskId, "PENDING", id); // record parse task for DOCX
            
            // Poll until the parse task completes (with timeout)
            TaskResponseDto parseTaskResponse = waitForTaskCompletion(parseTaskId);
            
            if (parseTaskResponse != null && "SUCCESS".equals(parseTaskResponse.getStatus())) {
                // Extract collection name and flash cards
                Map<String, Object> resultMap = (Map<String, Object>) parseTaskResponse.getResult();
                
                // Update per-file metadata to READY; collection already set to shared
                String collectionName = (String) resultMap.get("qdrant_collection_name");
                try {
                    uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                        if (existing.getCollectionName() == null && collectionName != null) {
                            existing.setCollectionName(collectionName);
                        }
                        existing.setStatus("READY");
                        uploadedFileService.save(existing);
                    });
                } catch (Exception e) {
                    System.err.println("Failed to update DOCX metadata to READY: " + e.getMessage());
                }
                
                // Extract and add flash cards
                List<Map<String, String>> flashCardsMap = (List<Map<String, String>>) resultMap.get("flash_cards");
                if (flashCardsMap != null && !flashCardsMap.isEmpty()) {
                    addFlashCardsToCourseWithSource(id, flashCardsMap, response.getFileId());
                }
            } else {
                uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                    existing.setStatus("FAILED");
                    uploadedFileService.save(existing);
                });
            }
        }
        
        // Return the updated course
        return ResponseEntity.ok(convertToDto(courseService.getCourseById(id).get()));
    }

    @PostMapping("/{id}/upload-video")
    public ResponseEntity<CourseDto> uploadVideo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        // Verify course exists
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Upload file and get initial response
        UploadResponseDto response = apiClient.uploadVideo(file).block();
        if (response == null || response.getTaskId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload video");
        }
        
        // Create task for tracking
        Task task = taskService.createTask(response.getTaskId(), "PENDING", id);

        // Early persist video file metadata
        try {
            com.example.demo.model.UploadedFile uf = new com.example.demo.model.UploadedFile();
            uf.setCourse(course);
            uf.setType("FILE_VIDEO");
            uf.setFilename(response.getFilename());
            uf.setFileId(response.getFileId());
            // Initially track the upload task; we'll update this id as subsequent tasks start
            uf.setProcessingTaskId(response.getTaskId());
            uploadedFileService.save(uf);
        } catch (Exception e) {
            System.err.println("Failed early save of VIDEO metadata: " + e.getMessage());
        }
        
        // Wait for upload task to complete
        TaskResponseDto uploadStatus = waitForTaskCompletion(response.getTaskId());
        
        if (uploadStatus != null && "SUCCESS".equals(uploadStatus.getStatus())) {
            // Extract video path from result
            String videoPath = extractVideoPath(uploadStatus);
            
            if (videoPath != null) {
                try {
                    // Create video URI request
                    VideoUriRequestDto videoUriRequest = new VideoUriRequestDto();
                    VideoDto video = new VideoDto();
                    video.setType("FILE_VIDEO");
                    video.setPath(videoPath);
                    videoUriRequest.setVideo(video);
                    videoUriRequest.setUriProvider("gemini_uri_provider");
                    
                    // Get video URI
                    VideoUriResponseDto videoUriResponse = apiClient.getVideoUri(videoUriRequest).block();
                    
                    if (videoUriResponse != null && videoUriResponse.getTaskId() != null) {
                        System.out.println("Received videoUriResponse with taskId: " + videoUriResponse.getTaskId());
                        // Update processing task id to reflect the current long-running stage
                        try {
                            final String fid = response.getFileId();
                            uploadedFileService.findById(fid).ifPresent(existing -> {
                                existing.setProcessingTaskId(videoUriResponse.getTaskId());
                                uploadedFileService.save(existing);
                            });
                        } catch (Exception ignore) {}
                        
                        // Poll with extended timeout
                        TaskResponseDto uriTaskStatus = null;
                        final int MAX_ATTEMPTS = 100000;
                        final int POLLING_INTERVAL_MS = 5000;
                        
                        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                            try {
                                TaskResponseDto status = apiClient.getTaskStatus(videoUriResponse.getTaskId()).block();
                                
                                if (status != null && 
                                    ("SUCCESS".equals(status.getStatus()) || "FAILURE".equals(status.getStatus()))) {
                                    uriTaskStatus = status;
                                    System.out.println("Video URI task completed with status: " + status.getStatus());
                                    break;
                                }
                                
                                System.out.println("Waiting for video URI task to complete, attempt " + (attempt + 1));
                                Thread.sleep(POLLING_INTERVAL_MS);
                            } catch (Exception e) {
                                System.err.println("Error polling task status: " + e.getMessage());
                            }
                        }
                        
                        if (uriTaskStatus != null && "SUCCESS".equals(uriTaskStatus.getStatus())) {
                            // Create Video object from response
                            Video video2 = createVideoFromResponse(uriTaskStatus);
                            if (video2 != null) {
                                course = courseService.setVideo(id, video2);
                                System.out.println("Successfully set video for course: " + id);
                                // Mark file READY now that processing completed
                                try {
                                    uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                                        existing.setStatus("READY");
                                        uploadedFileService.save(existing);
                                    });
                                } catch (Exception e) {
                                    System.err.println("Failed to update VIDEO metadata to READY: " + e.getMessage());
                                }
                                
                                // Now generate flash cards for the video
                                try {
                                    FlashCardRequestDto flashCardRequest = new FlashCardRequestDto();
                                    VideoDto videoDto = new VideoDto();
                                    videoDto.setType(video2.getType());
                                    videoDto.setUri(video2.getUri());
                                    videoDto.setPath(video2.getPath());
                                    
                                    if (video2.getDuration() != null) {
                                        VideoDto.DurationDto durationDto = new VideoDto.DurationDto();
                                        durationDto.setMinutes(video2.getDuration().getMinutes());
                                        durationDto.setSeconds(video2.getDuration().getSeconds());
                                        videoDto.setDuration(durationDto);
                                    }
                                    flashCardRequest.setVideo(videoDto);
                                    
                                    System.out.println("Requesting flash cards for video: " + videoDto);
                                    
                                    // Call API to get flash cards
                                    TaskResponseDto flashCardResponse = apiClient.createFlashCards(flashCardRequest).block();
                                    
                                    if (flashCardResponse != null && flashCardResponse.getTaskId() != null) {
                                        System.out.println("Received flash card task ID: " + flashCardResponse.getTaskId());
                                        // Update processing task id again to track flash card generation
                                        try {
                                            final String fid2 = response.getFileId();
                                            uploadedFileService.findById(fid2).ifPresent(existing -> {
                                                existing.setProcessingTaskId(flashCardResponse.getTaskId());
                                                uploadedFileService.save(existing);
                                            });
                                        } catch (Exception ignore) {}
                                        
                                        // Wait for flash cards task to complete
                                        TaskResponseDto flashCardTaskStatus = waitForTaskCompletion(flashCardResponse.getTaskId());
                                        
                                        if (flashCardTaskStatus != null && "SUCCESS".equals(flashCardTaskStatus.getStatus())) {
                                            System.out.println("Flash card task completed successfully");
                                            
                                            // Extract and add flash cards
                                            Map<String, Object> resultMap = (Map<String, Object>) flashCardTaskStatus.getResult();
                                            List<Map<String, String>> flashCardsMap = (List<Map<String, String>>) resultMap.get("flash_cards");
                                            
                                            if (flashCardsMap != null && !flashCardsMap.isEmpty()) {
                                                System.out.println("Found " + flashCardsMap.size() + " flash cards");
                                                addFlashCardsToCourseWithSource(id, flashCardsMap, response.getFileId());
                                            } else {
                                                System.out.println("No flash cards found in response");
                                            }
                                        } else {
                                            System.err.println("Flash card task failed or timed out: " + 
                                                (flashCardTaskStatus != null ? flashCardTaskStatus.getStatus() : "null"));
                                        }
                                    } else {
                                        System.err.println("Failed to get flash card task ID");
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error getting flash cards: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            System.err.println("URI task failed or timed out: " + 
                                (uriTaskStatus != null ? uriTaskStatus.getStatus() : "null"));
                            uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                                existing.setStatus("FAILED");
                                uploadedFileService.save(existing);
                            });
                        }
                    } else {
                        System.err.println("Failed to get video URI task ID");
                    }
                } catch (Exception e) {
                    System.err.println("Error getting video URI: " + e.getMessage());
                    e.printStackTrace();
                    uploadedFileService.findById(response.getFileId()).ifPresent(existing -> {
                        existing.setStatus("FAILED");
                        uploadedFileService.save(existing);
                    });
                }
            }
        }
        
        // Return the updated course
        return ResponseEntity.ok(convertToDto(courseService.getCourseById(id).get()));
    }

    @PostMapping("/{id}/flashcards/evaluate")
    public ResponseEntity<AnswerEvaluationResponseDto> evaluateFlashcardAnswer(
            @PathVariable Long id,
            @RequestBody AnswerEvaluationRequestDto request) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        String collectionName = course.getPdfCollectionName();
        if (collectionName == null || collectionName.isEmpty()) {
            collectionName = course.getDocxCollectionName();
        }

        AnswerEvaluationResponseDto result = apiClient
                .evaluateFlashCardAnswer(request, collectionName)
                .block();
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Evaluation failed");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/add-youtube-video")
    public ResponseEntity<CourseDto> addYoutubeVideo(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        // Verify course exists
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Get YouTube URL from payload
        String youtubeUrl = payload.get("youtubeUrl");
        if (youtubeUrl == null || youtubeUrl.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YouTube URL is required");
        }
        
        try {
            // Create Video object for YouTube without URI data
            Video video = new Video();
            video.setType("YOUTUBE_VIDEO");
            video.setUri(youtubeUrl);
            // Don't set uriData for YouTube videos
            
            // Save the video to the course
            course = courseService.setVideo(id, video);

            // Create a metadata entry so YouTube video appears in uploaded files list
            String youtubeFileId = UUID.randomUUID().toString();
            try {
                com.example.demo.model.UploadedFile uf = new com.example.demo.model.UploadedFile();
                uf.setCourse(course);
                uf.setType("YOUTUBE_VIDEO");
                uf.setFilename(youtubeUrl);
                uf.setFileId(youtubeFileId);
                uploadedFileService.save(uf);
            } catch (Exception e) {
                System.err.println("Failed early save of YOUTUBE metadata: " + e.getMessage());
            }
            
            // Get flash cards for the YouTube video
            FlashCardRequestDto flashCardRequest = new FlashCardRequestDto();
            VideoDto videoDto = new VideoDto();
            videoDto.setType("YOUTUBE_VIDEO");
            videoDto.setUri(youtubeUrl);
            flashCardRequest.setVideo(videoDto);
            
            // Call API to get flash cards
            TaskResponseDto flashCardResponse = apiClient.createFlashCards(flashCardRequest).block();
            if (flashCardResponse != null && flashCardResponse.getTaskId() != null) {
                // Track the flash card task id for this YouTube file
                try {
                    uploadedFileService.findById(youtubeFileId).ifPresent(existing -> {
                        existing.setProcessingTaskId(flashCardResponse.getTaskId());
                        uploadedFileService.save(existing);
                    });
                } catch (Exception ignore) {}
                // Wait for flash cards task to complete
                TaskResponseDto flashCardTaskStatus = waitForTaskCompletion(flashCardResponse.getTaskId());
                
                if (flashCardTaskStatus != null && "SUCCESS".equals(flashCardTaskStatus.getStatus())) {
                    // Extract and add flash cards
                    Map<String, Object> resultMap = (Map<String, Object>) flashCardTaskStatus.getResult();
                    List<Map<String, String>> flashCardsMap = (List<Map<String, String>>) resultMap.get("flash_cards");
                    if (flashCardsMap != null && !flashCardsMap.isEmpty()) {
                        addFlashCardsToCourseWithSource(id, flashCardsMap, youtubeFileId);
                    }
                    uploadedFileService.findById(youtubeFileId).ifPresent(existing -> {
                        existing.setStatus("READY");
                        uploadedFileService.save(existing);
                    });
                } else {
                    uploadedFileService.findById(youtubeFileId).ifPresent(existing -> {
                        existing.setStatus("FAILED");
                        uploadedFileService.save(existing);
                    });
                }
            }
            
            return ResponseEntity.ok(convertToDto(courseService.getCourseById(id).get()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to add YouTube video: " + e.getMessage());
        }
    }
        
    @PostMapping("/{id}/pdf-collection")
    public ResponseEntity<CourseDto> setPdfCollection(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String collectionName = payload.get("collectionName");
        if (collectionName == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return courseService.getCourseById(id)
                .map(course -> {
                    Course updatedCourse = courseService.setPdfCollectionName(id, collectionName);
                    return ResponseEntity.ok(convertToDto(updatedCourse));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/docx-collection")
    public ResponseEntity<CourseDto> setDocxCollection(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String collectionName = payload.get("collectionName");
        if (collectionName == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return courseService.getCourseById(id)
                .map(course -> {
                    Course updatedCourse = courseService.setDocxCollectionName(id, collectionName);
                    return ResponseEntity.ok(convertToDto(updatedCourse));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/set-video")
    public ResponseEntity<CourseDto> setVideo(@PathVariable Long id, @RequestBody VideoDto videoDto) {
        return courseService.getCourseById(id)
                .map(course -> {
                    Video video = new Video(videoDto.getType(), videoDto.getUri());
                    video.setPath(videoDto.getPath());
                    
                    if (videoDto.getDuration() != null) {
                        Duration duration = new Duration(
                                videoDto.getDuration().getMinutes(),
                                videoDto.getDuration().getSeconds()
                        );
                        video.setDuration(duration);
                    }
                    
                    Course updatedCourse = courseService.setVideo(id, video);
                    return ResponseEntity.ok(convertToDto(updatedCourse));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    private CourseDto convertToDto(Course course) {
        CourseDto dto = new CourseDto();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setCreatedAt(course.getCreatedAt());
        
        if (course.getOwner() != null) {
            dto.setOwnerId(course.getOwner().getId());
            dto.setOwnerUsername(course.getOwner().getUsername());
        }
        
        dto.setPdfCollectionName(course.getPdfCollectionName());
        dto.setDocxCollectionName(course.getDocxCollectionName());
        
        if (course.getVideo() != null) {
            VideoDto videoDto = new VideoDto();
            videoDto.setType(course.getVideo().getType());
            videoDto.setUri(course.getVideo().getUri());
            videoDto.setPath(course.getVideo().getPath());
            videoDto.setUriData(course.getVideo().getUriData());
            
            if (course.getVideo().getDuration() != null) {
                VideoDto.DurationDto durationDto = new VideoDto.DurationDto();
                durationDto.setMinutes(course.getVideo().getDuration().getMinutes());
                durationDto.setSeconds(course.getVideo().getDuration().getSeconds());
                videoDto.setDuration(durationDto);
            }
            
            dto.setVideo(videoDto);
        }
        
        List<FlashCardDto> flashCardDtos = course.getFlashCards().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        dto.setFlashCards(flashCardDtos);

        // Attach course questions
        try {
            List<CourseQuestion> questions = courseQuestionService.findByCourse(course);
            List<CourseQuestionDto> questionDtos = questions.stream().map(q -> {
                CourseQuestionDto qDto = new CourseQuestionDto();
                qDto.setId(q.getId());
                qDto.setFileId(q.getFileId());
                qDto.setQuestionText(q.getQuestionText());
                qDto.setAnswersJson(normalizeAnswersJson(q.getAnswersJson()));
                qDto.setImagesJson(q.getImagesJson());
                return qDto;
            }).collect(Collectors.toList());
            dto.setQuestions(questionDtos);
        } catch (Exception e) {
            System.err.println("Failed attaching course questions: " + e.getMessage());
        }
        
        return dto;
    }

    /**
     * Normalizes legacy answers JSON (array of objects including a guidelines object) into
     * the new dictionary structure:
     * {
     *   "type": "normal" | "table",
     *   "guidelines": "..."?,
     *   "ai_reasoning": "..."? (only when type == table),
     *   "questions": [ { "question":..., "correct_answer":..., "ai_reasoning":... }, ... ]
     * }
     * If already in new structure (object with 'questions' key) it is returned unchanged.
     */
    private String normalizeAnswersJson(String answersJson) {
        if (answersJson == null || answersJson.isEmpty()) return answersJson;
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(answersJson);
            // Already new structure
            if (root.isObject() && root.has("questions")) {
                return answersJson; // pass through
            }
            if (root.isArray()) {
                ArrayNode arr = (ArrayNode) root;
                ObjectNode out = mapper.createObjectNode();
                out.put("type", "normal");
                ArrayNode questionsArr = mapper.createArrayNode();
                String guidelinesValue = null;
                for (JsonNode node : arr) {
                    if (node.has("guidelines")) {
                        // guidelines node
                        if (guidelinesValue == null) guidelinesValue = node.get("guidelines").asText();
                        continue;
                    }
                    // Treat remaining objects as question entries
                    ObjectNode qObj = mapper.createObjectNode();
                    if (node.has("question")) qObj.set("question", node.get("question"));
                    if (node.has("correct_answer")) qObj.set("correct_answer", node.get("correct_answer"));
                    if (node.has("ai_reasoning")) qObj.set("ai_reasoning", node.get("ai_reasoning"));
                    questionsArr.add(qObj);
                }
                if (guidelinesValue != null) out.put("guidelines", guidelinesValue);
                out.set("questions", questionsArr);
                return mapper.writeValueAsString(out);
            }
            // Unknown shape, return original
            return answersJson;
        } catch (Exception e) {
            // On parse failure, return original to avoid data loss
            return answersJson;
        }
    }
    
    private FlashCardDto convertToDto(FlashCard flashCard) {
        FlashCardDto dto = new FlashCardDto();
        dto.setId(flashCard.getId());
        dto.setQuestion(flashCard.getQuestion());
        dto.setAnswer(flashCard.getAnswer());
        return dto;
    }

    private String waitForCollectionName(String taskId) {
        final int MAX_ATTEMPTS = 100000;
        final int POLLING_INTERVAL_MS = 5000;
        
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                // Poll task status
                TaskResponseDto taskStatus = apiClient.getTaskStatus(taskId).block();
                
                if (taskStatus != null && "SUCCESS".equals(taskStatus.getStatus())) {
                    // Extract qdrant_collection_name from result
                    if (taskStatus.getResult() instanceof Map) {
                        Map<String, Object> resultMap = (Map<String, Object>) taskStatus.getResult();
                        return (String) resultMap.get("qdrant_collection_name");
                    }
                    return null;
                } else if (taskStatus != null && "FAILURE".equals(taskStatus.getStatus())) {
                    // Task failed, no need to keep waiting
                    return null;
                }
                
                // Sleep before next attempt
                Thread.sleep(POLLING_INTERVAL_MS);
            } catch (Exception e) {
                // Log exception and continue polling
                System.err.println("Error polling task status: " + e.getMessage());
            }
        }
        
        // Timeout reached
        return null;
    }

    private TaskResponseDto waitForTaskCompletion(String taskId) {
        final int MAX_ATTEMPTS = 100000;
        final int POLLING_INTERVAL_MS = 5000;
        
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                TaskResponseDto taskStatus = apiClient.getTaskStatus(taskId).block();
                
                if (taskStatus != null && 
                    ("SUCCESS".equals(taskStatus.getStatus()) || "FAILURE".equals(taskStatus.getStatus()))) {
                    return taskStatus;
                }
                
                Thread.sleep(POLLING_INTERVAL_MS);
            } catch (Exception e) {
                System.err.println("Error polling task status: " + e.getMessage());
            }
        }
        
        return null;
    }
    

    private String extractVideoPath(TaskResponseDto taskStatus) {
        try {
            if (taskStatus.getResult() instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) taskStatus.getResult();
                return (String) resultMap.get("path");
            }
        } catch (Exception e) {
            System.err.println("Error extracting video path: " + e.getMessage());
        }
        return null;
    }

    private Video createVideoFromResponse(TaskResponseDto taskStatus) {
        try {
            if (taskStatus.getResult() instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) taskStatus.getResult();
                Object videoObj = resultMap.get("video");
                
                if (videoObj instanceof Map) {
                    Map<String, Object> videoMap = (Map<String, Object>) videoObj;
                    
                    Video video = new Video();
                    video.setType((String) videoMap.get("type"));
                    video.setUri((String) videoMap.get("uri"));
                    video.setPath((String) videoMap.get("path"));
                    
                    // Set duration if available
                    Object durationObj = videoMap.get("duration");
                    if (durationObj instanceof Map) {
                        Map<String, Integer> durationMap = (Map<String, Integer>) durationObj;
                        Duration duration = new Duration(
                                durationMap.get("minutes"),
                                durationMap.get("seconds")
                        );
                        video.setDuration(duration);
                    }
                    
                    // Set URI data if available
                    Object uriDataObj = videoMap.get("uri_data");
                    if (uriDataObj instanceof Map) {
                        Map<String, String> uriData = (Map<String, String>) uriDataObj;
                        video.setUriData(uriData);
                    }
                    
                    return video;
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating video from response: " + e.getMessage());
        }
        return null;
    }

    private void addFlashCardsToCourse(Long courseId, List<Map<String, String>> flashCardsMap) {
        for (Map<String, String> cardMap : flashCardsMap) {
            FlashCard flashCard = new FlashCard();
            flashCard.setQuestion(cardMap.get("question"));
            flashCard.setAnswer(cardMap.get("answer"));
            courseService.addFlashCard(courseId, flashCard);
        }
    }

    private void addFlashCardsToCourseWithSource(Long courseId, List<Map<String, String>> flashCardsMap, String sourceFileId) {
        com.example.demo.model.UploadedFile sourceFile = null;
        if (sourceFileId != null) {
            sourceFile = uploadedFileService.findById(sourceFileId).orElse(null);
        }
        for (Map<String, String> cardMap : flashCardsMap) {
            FlashCard flashCard = new FlashCard();
            flashCard.setQuestion(cardMap.get("question"));
            flashCard.setAnswer(cardMap.get("answer"));
            if (sourceFile != null) {
                flashCard.setUploadedFile(sourceFile);
            }
            courseService.addFlashCard(courseId, flashCard);
        }
    }

    // List tasks for a course (for persistent upload status)
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<com.example.demo.model.Task>> getTasksForCourse(@PathVariable Long id) {
        return courseService.getCourseById(id)
                .map(course -> ResponseEntity.ok(taskService.getTasksForCourse(course)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Proxy task status by id so frontend can poll per-file processing state
    @GetMapping("/{id}/task-status/{taskId}")
    public ResponseEntity<TaskResponseDto> getTaskStatus(@PathVariable Long id, @PathVariable String taskId) {
        if (!courseService.getCourseById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        try {
            TaskResponseDto status = apiClient.getTaskStatus(taskId).block();
            if (status == null) return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    // List uploaded files for a course
    @GetMapping("/{id}/uploaded-files")
    public ResponseEntity<List<com.example.demo.dto.UploadedFileDto>> getUploadedFiles(@PathVariable Long id) {
        if (!courseService.getCourseById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<com.example.demo.model.UploadedFile> files = uploadedFileService.findByCourseId(id);
        List<com.example.demo.dto.UploadedFileDto> dtos = files.stream().map(f -> {
            com.example.demo.dto.UploadedFileDto dto = new com.example.demo.dto.UploadedFileDto();
            dto.setFileId(f.getFileId());
            dto.setFilename(f.getFilename());
            dto.setType(f.getType());
            dto.setCollectionName(f.getCollectionName());
            dto.setStatus(f.getStatus() == null ? "READY" : f.getStatus());
            dto.setProcessingTaskId(f.getProcessingTaskId());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Fetch questions for a specific XML file
    @GetMapping("/{id}/questions/{fileId}")
    public ResponseEntity<List<CourseQuestionDto>> getXmlQuestions(@PathVariable Long id, @PathVariable String fileId) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        // Validate file belongs to course and is XML
        com.example.demo.model.UploadedFile file = uploadedFileService.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (file.getCourse() == null || !course.getId().equals(file.getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File does not belong to this course");
        }
        if (!"XML".equalsIgnoreCase(file.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not an XML quiz");
        }
        List<CourseQuestion> questions = courseQuestionService.findByFileId(fileId);
        List<CourseQuestionDto> dtos = questions.stream().map(q -> {
            CourseQuestionDto dto = new CourseQuestionDto();
            dto.setId(q.getId());
            dto.setFileId(q.getFileId());
            dto.setQuestionText(q.getQuestionText());
            dto.setAnswersJson(q.getAnswersJson());
            dto.setImagesJson(q.getImagesJson());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Delete XML file and its questions
    @DeleteMapping("/{id}/materials/xml/{fileId}")
    public ResponseEntity<Map<String,Object>> deleteXmlFile(@PathVariable Long id, @PathVariable String fileId) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        com.example.demo.model.UploadedFile file = uploadedFileService.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (file.getCourse() == null || !course.getId().equals(file.getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File does not belong to this course");
        }
        if (!"XML".equalsIgnoreCase(file.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not XML");
        }
        // Remove associated questions
        try { courseQuestionService.deleteByFileId(fileId); } catch (Exception ignore) {}
        // Remove metadata
        uploadedFileService.deleteByFileId(fileId);
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    // Delete a specific PDF file's points from the course collection
    @DeleteMapping("/{id}/materials/pdf/{fileId}")
    public ResponseEntity<Map<String,Object>> deletePdfFile(@PathVariable Long id, @PathVariable String fileId) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Validate uploaded file exists and belongs to this course and is of type PDF
        com.example.demo.model.UploadedFile file = uploadedFileService.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (file.getCourse() == null || !course.getId().equals(file.getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File does not belong to this course");
        }
        if (!"PDF".equalsIgnoreCase(file.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a PDF");
        }

        String collectionName = file.getCollectionName();
        if (collectionName == null || collectionName.isEmpty()) {
            // Fallback to course-level collection if per-file missing
            collectionName = course.getPdfCollectionName();
        }
        if (collectionName == null || collectionName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No PDF collection available for deletion");
        }

        Map<String,Object> resp = apiClient.deleteFileFromCollection(collectionName, fileId).block();

        // If Celery task is returned, delete metadata and source flash cards immediately and acknowledge
        if (resp != null && resp.get("task_id") instanceof String) {
            String taskId = (String) resp.get("task_id");
            try { flashCardRepository.deleteByUploadedFileId(fileId); } catch (Exception ignore) {}
            uploadedFileService.deleteByFileId(fileId);
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "task_id", taskId,
                "message", "Deletion enqueued and metadata removed"
            ));
        }

        // Backward compatibility: immediate success response
        if (resp != null && "SUCCESS".equals(resp.get("status"))) {
            try { flashCardRepository.deleteByUploadedFileId(fileId); } catch (Exception ignore) {}
            uploadedFileService.deleteByFileId(fileId);
            return ResponseEntity.ok(resp);
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected deletion response");
    }

    // Delete a specific DOCX file's points from the course collection
    @DeleteMapping("/{id}/materials/docx/{fileId}")
    public ResponseEntity<Map<String,Object>> deleteDocxFile(@PathVariable Long id, @PathVariable String fileId) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Validate uploaded file exists and belongs to this course and is of type DOCX
        com.example.demo.model.UploadedFile file = uploadedFileService.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (file.getCourse() == null || !course.getId().equals(file.getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File does not belong to this course");
        }
        if (!"DOCX".equalsIgnoreCase(file.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a DOCX");
        }

        String collectionName = file.getCollectionName();
        if (collectionName == null || collectionName.isEmpty()) {
            // Fallback to course-level collection if per-file missing
            collectionName = course.getDocxCollectionName();
        }
        if (collectionName == null || collectionName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No DOCX collection available for deletion");
        }

        Map<String,Object> resp = apiClient.deleteFileFromCollection(collectionName, fileId).block();

        // If Celery task is returned, delete metadata and source flash cards immediately and acknowledge
        if (resp != null && resp.get("task_id") instanceof String) {
            String taskId = (String) resp.get("task_id");
            try { flashCardRepository.deleteByUploadedFileId(fileId); } catch (Exception ignore) {}
            uploadedFileService.deleteByFileId(fileId);
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "task_id", taskId,
                "message", "Deletion enqueued and metadata removed"
            ));
        }

        // Backward compatibility: immediate success response
        if (resp != null && "SUCCESS".equals(resp.get("status"))) {
            try { flashCardRepository.deleteByUploadedFileId(fileId); } catch (Exception ignore) {}
            uploadedFileService.deleteByFileId(fileId);
            return ResponseEntity.ok(resp);
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected deletion response");
    }

    // Delete a video or YouTube entry from the course (no Qdrant cleanup needed)
    @DeleteMapping("/{id}/materials/video/{fileId}")
    public ResponseEntity<Map<String,Object>> deleteVideoFile(@PathVariable Long id, @PathVariable String fileId) {
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        com.example.demo.model.UploadedFile file = uploadedFileService.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (file.getCourse() == null || !course.getId().equals(file.getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "File does not belong to this course");
        }
        String type = file.getType() != null ? file.getType().toUpperCase() : "";
        if (!type.equals("FILE_VIDEO") && !type.equals("YOUTUBE_VIDEO")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a video");
        }

        // Clear course video if it appears to match this entry
        try {
            if (course.getVideo() != null) {
                if (type.equals("FILE_VIDEO") && course.getVideo().getPath() != null && course.getVideo().getPath().contains(fileId)) {
                    courseService.clearVideo(id);
                } else if (type.equals("YOUTUBE_VIDEO") && course.getVideo().getUri() != null && course.getVideo().getUri().equals(file.getFilename())) {
                    courseService.clearVideo(id);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to clear course video: " + e.getMessage());
        }

        // Delete flash cards produced from this source
    try { flashCardRepository.deleteByUploadedFileId(fileId); } catch (Exception ignore) {}
        // Delete metadata
        uploadedFileService.deleteByFileId(fileId);

        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }

    @PostMapping("/{id}/chat")
    public ResponseEntity<List<MessageDto>> chatWithCourse(@PathVariable Long id, @RequestBody MessageDto userMessage) {
        // Verify course exists
        Course course = courseService.getCourseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        // Print debug info
        System.out.println("Processing chat for course ID: " + id);
        
        // Create new user message
        Message newMessage = new Message();
        newMessage.setContent(userMessage.getContent());
        newMessage.setRole("user");
        newMessage.setCourse(course); 
        newMessage.setTimestamp(LocalDateTime.now());
        
        // Save and verify
        Message savedMessage = messageService.saveMessage(newMessage);
        System.out.println("Saved user message ID: " + savedMessage.getId() + 
                        ", Course ID: " + (savedMessage.getCourse() != null ? savedMessage.getCourse().getId() : "null"));
        
        // Create process AI request
        ProcessAiRequestDto processAiRequest = new ProcessAiRequestDto();
        processAiRequest.setModelProvider("gemini");
        processAiRequest.setModelName("gemini-2.0-flash");
        processAiRequest.setSystemInstructionText("You are a helpful assistant that answers questions about the course materials. Be concise but thorough.");
        
        // Add conversation history
        List<Message> existingMessages = messageService.getMessagesByCourse(id);
        
        // Log the conversation history to debug
        System.out.println("Existing messages before AI call: " + existingMessages.size());
        for (Message msg : existingMessages) {
            System.out.println("  Message ID: " + msg.getId() + ", Role: " + msg.getRole() + ", Content: " + 
                            msg.getContent().substring(0, Math.min(20, msg.getContent().length())) + "...");
        }
        
        ConversationHistoryDto conversationHistory = new ConversationHistoryDto();
        List<MessageDto> messageDtos = existingMessages.stream()
                .map(msg -> {
                    MessageDto dto = new MessageDto();
                    dto.setRole(msg.getRole());
                    dto.setContent(msg.getContent());
                    return dto;
                })
                .collect(Collectors.toList());
        conversationHistory.setMessages(messageDtos);
        processAiRequest.setConversationHistory(conversationHistory);
        
        // Add course materials
        if (course.getPdfCollectionName() != null) {
            processAiRequest.setPdfCollectionName(course.getPdfCollectionName());
        }
        if (course.getDocxCollectionName() != null) {
            processAiRequest.setDocxCollectionName(course.getDocxCollectionName());
        }
        if (course.getVideo() != null) {
            VideoDto videoDto = new VideoDto();
            videoDto.setType(course.getVideo().getType());
            videoDto.setUri(course.getVideo().getUri());
            videoDto.setPath(course.getVideo().getPath());
            
            if (course.getVideo().getDuration() != null) {
                VideoDto.DurationDto durationDto = new VideoDto.DurationDto();
                durationDto.setMinutes(course.getVideo().getDuration().getMinutes());
                durationDto.setSeconds(course.getVideo().getDuration().getSeconds());
                videoDto.setDuration(durationDto);
            }
            processAiRequest.setVideo(videoDto);
        }
        
        Message aiMessage = null;
        
        try {
            // Call the API
            ProcessAiResponseDto response = apiClient.processAi(processAiRequest).block();
            
            if (response != null && response.getTaskId() != null) {
                System.out.println("Got task ID: " + response.getTaskId());
                TaskResponseDto taskStatus = waitForTaskCompletion(response.getTaskId());
                
                if (taskStatus != null && "SUCCESS".equals(taskStatus.getStatus())) {
                    System.out.println("Task completed successfully");
                    
                    // Print the entire result to debug
                    System.out.println("Task result: " + taskStatus.getResult());
                    
                    if (taskStatus.getResult() instanceof Map) {
                        Map<String, Object> resultMap = (Map<String, Object>) taskStatus.getResult();
                        
                        // Check if conversation_history is a direct array rather than a nested object
                        Object convHistoryObj = resultMap.get("conversation_history");
                        
                        if (convHistoryObj instanceof List) {
                            // This is the case we're seeing in your logs - direct array
                            List<Map<String, Object>> messagesList = (List<Map<String, Object>>) convHistoryObj;
                            if (!messagesList.isEmpty()) {
                                // Get the last message in the array (the AI response)
                                Map<String, Object> lastMessage = messagesList.get(messagesList.size() - 1);
                                
                                // Only process if it's a model message
                                if ("model".equals(lastMessage.get("role"))) {
                                    String content = (String) lastMessage.get("content");
                                    String role = (String) lastMessage.get("role");
                                    
                                    // Create and save the AI response
                                    aiMessage = new Message();
                                    aiMessage.setContent(content);
                                    aiMessage.setRole(role);  // Keep the original "model" role
                                    aiMessage.setCourse(course);
                                    aiMessage.setTimestamp(LocalDateTime.now());
                                    
                                    Message savedAiMessage = messageService.saveMessage(aiMessage);
                                    System.out.println("Saved AI message ID: " + savedAiMessage.getId() + 
                                                    ", Course ID: " + (savedAiMessage.getCourse() != null ? 
                                                                        savedAiMessage.getCourse().getId() : "null"));
                                }
                            }
                        } else {
                            // Try other formats - this is the nested structure approach
                            Object conversationObj = resultMap.get("conversation_history");
                            if (conversationObj == null) {
                                conversationObj = resultMap.get("conversationHistory");
                            }
                            
                            if (conversationObj == null) {
                                // Try to extract messages directly from the result
                                Object messagesObj = resultMap.get("messages");
                                if (messagesObj instanceof List) {
                                    List<Map<String, String>> messagesList = (List<Map<String, String>>) messagesObj;
                                    if (!messagesList.isEmpty()) {
                                        Map<String, String> lastMessage = messagesList.get(messagesList.size() - 1);
                                        String role = lastMessage.get("role");
                                        String content = lastMessage.get("content");
                                        
                                        if (role != null && content != null) {
                                            aiMessage = new Message();
                                            aiMessage.setContent(content);
                                            aiMessage.setRole(role);
                                            aiMessage.setCourse(course);
                                            aiMessage.setTimestamp(LocalDateTime.now());
                                            
                                            Message savedAiMessage = messageService.saveMessage(aiMessage);
                                            System.out.println("Saved AI message ID: " + savedAiMessage.getId() + 
                                                            ", Course ID: " + (savedAiMessage.getCourse() != null ? 
                                                                            savedAiMessage.getCourse().getId() : "null"));
                                        }
                                    }
                                } else {
                                    System.out.println("No conversation history or messages found in response");
                                    // Create a default AI response
                                    String content = "I've processed your request about the course materials.";
                                    aiMessage = createErrorMessage(course, content);
                                }
                            } else if (conversationObj instanceof Map) {
                                Map<String, Object> conversationMap = (Map<String, Object>) conversationObj;
                                Object messagesObj = conversationMap.get("messages");
                                
                                if (messagesObj instanceof List) {
                                    List<Map<String, String>> messagesList = (List<Map<String, String>>) messagesObj;
                                    if (!messagesList.isEmpty()) {
                                        Map<String, String> lastMessage = messagesList.get(messagesList.size() - 1);
                                        String role = lastMessage.get("role");
                                        String content = lastMessage.get("content");
                                        
                                        if (role != null && content != null) {
                                            aiMessage = new Message();
                                            aiMessage.setContent(content);
                                            aiMessage.setRole(role);
                                            aiMessage.setCourse(course);
                                            aiMessage.setTimestamp(LocalDateTime.now());
                                            
                                            Message savedAiMessage = messageService.saveMessage(aiMessage);
                                            System.out.println("Saved AI message ID: " + savedAiMessage.getId() + 
                                                            ", Course ID: " + (savedAiMessage.getCourse() != null ? 
                                                                            savedAiMessage.getCourse().getId() : "null"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("Task failed with status: " + (taskStatus != null ? taskStatus.getStatus() : "null"));
                    aiMessage = createErrorMessage(course, "Sorry, I couldn't process your request at this time.");
                }
            } else {
                System.out.println("No task ID received");
                aiMessage = createErrorMessage(course, "Sorry, I couldn't process your request at this time.");
            }
        } catch (Exception e) {
            System.err.println("Error in chat process: " + e.getMessage());
            aiMessage = createErrorMessage(course, "Sorry, an error occurred: " + e.getMessage());
        }
        
        // Get all messages AFTER processing, including the new AI response
        System.out.println("Fetching updated messages");
        List<Message> updatedMessages = messageService.getMessagesByCourse(id);
        
        // Log the final conversation history
        System.out.println("Updated messages after AI call: " + updatedMessages.size());
        for (Message msg : updatedMessages) {
            System.out.println("  Message ID: " + msg.getId() + ", Role: " + msg.getRole() + ", Content: " + 
                            msg.getContent().substring(0, Math.min(20, msg.getContent().length())) + "...");
        }
        
        return ResponseEntity.ok(updatedMessages.stream()
                .map(this::convertMessageToDto)
                .collect(Collectors.toList()));
    }

    // Helper method to handle messages list
    private void handleMessagesList(List<Map<String, String>> messagesList, Course course) {
        if (!messagesList.isEmpty()) {
            // Get the last message (AI response)
            Map<String, String> lastMessage = messagesList.get(messagesList.size() - 1);
            String roleFromApi = lastMessage.get("role");
            String roleToSave = "model".equals(roleFromApi) ? "assistant" : roleFromApi;
            String content = lastMessage.get("content");
            
            System.out.println("Got AI response with role: " + roleFromApi + ", content: " + 
                            content.substring(0, Math.min(50, content.length())) + "...");

            // Create and save the AI response
            Message aiMessage = new Message();
            aiMessage.setContent(content);
            aiMessage.setRole(roleToSave); 
            aiMessage.setCourse(course);
            aiMessage.setTimestamp(LocalDateTime.now());
            
            Message savedAiMessage = messageService.saveMessage(aiMessage);
            System.out.println("Saved AI message ID: " + savedAiMessage.getId() + 
                            ", Course ID: " + (savedAiMessage.getCourse() != null ? 
                                            savedAiMessage.getCourse().getId() : "null"));
        }
    }

    private Message createErrorMessage(Course course, String content) {
        Message errorMessage = new Message();
        errorMessage.setContent(content);
        errorMessage.setRole("model");  // Using "model" role for consistency
        errorMessage.setCourse(course);
        errorMessage.setTimestamp(LocalDateTime.now());
        return messageService.saveMessage(errorMessage);
    }

        // Get conversation history for a course
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageDto>> getConversationHistory(@PathVariable Long id) {
        // Verify course exists
        if (!courseService.getCourseById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        List<Message> messages = messageService.getMessagesByCourse(id);
        List<MessageDto> messageDtos = messages.stream()
                .map(this::convertMessageToDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(messageDtos);
    }

    // Add a message to the conversation history
    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageDto> addMessage(@PathVariable Long id, @RequestBody MessageDto messageDto) {
        return courseService.getCourseById(id)
                .map(course -> {
                    Message message = new Message();
                    message.setRole(messageDto.getRole());
                    message.setContent(messageDto.getContent());
                    message.setCourse(course);
                    
                    Message savedMessage = messageService.saveMessage(message);
                    return ResponseEntity.status(HttpStatus.CREATED).body(convertMessageToDto(savedMessage));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Helper method to convert Message to MessageDto
    private MessageDto convertMessageToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        if (message.getCourse() != null) {
            dto.setCourseId(message.getCourse().getId());
        }
        return dto;
    }

    
}