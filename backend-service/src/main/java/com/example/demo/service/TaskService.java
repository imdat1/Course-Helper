package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.client.ApiClient;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.dto.UploadResponseDto;
import com.example.demo.dto.VideoDto;
import com.example.demo.dto.VideoUriRequestDto;
import com.example.demo.dto.VideoUriResponseDto;
import com.example.demo.model.Task;
import com.example.demo.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    
    private final TaskRepository taskRepository;
    private final ApiClient apiClient;
    private final CourseService courseService;
    
    @Autowired
    public TaskService(TaskRepository taskRepository, ApiClient apiClient, CourseService courseService) {
        this.taskRepository = taskRepository;
        this.apiClient = apiClient;
        this.courseService = courseService;
    }
    
    public Task createTask(String taskId, String status, Long courseId) {
        Task task = new Task(taskId, status);
        courseService.getCourseById(courseId).ifPresent(task::setCourse);
        return taskRepository.save(task);
    }
    
    public Task updateTaskStatus(String taskId, String status, String result) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        task.setStatus(status);
        task.setResult(result);
        
        if ("SUCCESS".equals(status) || "FAILURE".equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
        }
        
        return taskRepository.save(task);
    }

    public List<Task> getTasksForCourse(com.example.demo.model.Course course) {
        return taskRepository.findByCourse(course);
    }
    
    // Run every 30 seconds to check for pending tasks
    @Scheduled(fixedDelay = 30000)
    public void checkPendingTasks() {
        logger.info("Checking pending tasks...");
        List<Task> pendingTasks = taskRepository.findByStatus("PENDING");
        logger.info("Found {} pending tasks", pendingTasks.size());
        
        for (Task task : pendingTasks) {
            try {
                logger.info("Checking task status for: {}", task.getTaskId());
                TaskResponseDto taskStatus = apiClient.getTaskStatus(task.getTaskId()).block();
                
                if (taskStatus == null) {
                    logger.warn("Received null response for task: {}", task.getTaskId());
                    continue;
                }
                
                logger.info("Task {} status: {}", task.getTaskId(), taskStatus.getStatus());
                
                if (!"PENDING".equals(taskStatus.getStatus())) {
                    // Convert result to string if it's not already
                    String resultStr = taskStatus.getResult() != null ? 
                        (taskStatus.getResult() instanceof String ? 
                            (String) taskStatus.getResult() : 
                            new ObjectMapper().writeValueAsString(taskStatus.getResult())) : 
                        null;
                        
                    updateTaskStatus(task.getTaskId(), taskStatus.getStatus(), resultStr);
                    
                    // If task was successful and has a course associated with it, update the course
                    if ("SUCCESS".equals(taskStatus.getStatus()) && task.getCourse() != null) {
                        processCompletedTask(task, taskStatus);
                    }
                }
            } catch (Exception e) {
                logger.error("Error checking task status for " + task.getTaskId(), e);
            }
        }
    }
    
    private void processCompletedTask(Task task, TaskResponseDto taskStatus) {
        if (task.getResult() != null && task.getResult().contains("parseSummariseIngestTaskId")) {
            // This is a PDF/DOCX upload task, we need to check the parse task
            checkParseSummariseTask(task);
        } else if (task.getResult() != null && task.getResult().contains("path")) {
            // This is a video upload task, we need to initiate video URI retrieval
            initiateVideoUriRequest(task);
        } else if (task.getResult() != null && task.getResult().contains("uri")) {
            // This is a video URI task that completed successfully
            updateCourseWithVideoUri(task);
        }
    }
    
    private void checkParseSummariseTask(Task uploadTask) {
        try {
            // Extract the parseSummariseIngestTaskId from the result
            UploadResponseDto uploadResult = parseUploadResponse(uploadTask.getResult());
            
            if (uploadResult != null && uploadResult.getParseSummariseIngestTaskId() != null) {
                // Check if we already have a task for this
                String parseTaskId = uploadResult.getParseSummariseIngestTaskId();
                
                // Check if we're already tracking this parse task
                if (!taskRepository.existsById(parseTaskId)) {
                    // Create a new task to track the parse task
                    Task parseTask = new Task(parseTaskId, "PENDING");
                    parseTask.setCourse(uploadTask.getCourse());
                    taskRepository.save(parseTask);
                    logger.info("Created new parse task to track: " + parseTaskId);
                } else {
                    // We're already tracking it, check its status
                    TaskResponseDto parseTaskStatus = apiClient.getTaskStatus(parseTaskId).block();
                    
                    if (parseTaskStatus != null && "SUCCESS".equals(parseTaskStatus.getStatus())) {
                        // Parse task completed successfully, update course with collection name
                        updateCourseWithCollectionName(uploadTask.getCourse().getId(), parseTaskStatus);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error processing parse task for upload: " + uploadTask.getTaskId(), e);
        }
    }
    
    private void initiateVideoUriRequest(Task uploadTask) {
    try {
        // Extract the video path from the result
        UploadResponseDto uploadResult = parseUploadResponse(uploadTask.getResult());
        
        if (uploadResult != null && uploadResult.getPath() != null) {
            // Initiate video URI request
            VideoUriRequestDto videoUriRequest = new VideoUriRequestDto();
            VideoDto video = new VideoDto();
            video.setType("FILE_VIDEO");
            video.setPath(uploadResult.getPath());
            videoUriRequest.setVideo(video);
            videoUriRequest.setUriProvider("gemini_uri_provider");
            // API key will be set by the ApiClient
            
            // Call the API to get video URI
            VideoUriResponseDto videoUriResponse = apiClient.getVideoUri(videoUriRequest).block();
            
            if (videoUriResponse != null && videoUriResponse.getTaskId() != null) {
                // Create task to track video URI request
                Task videoUriTask = new Task(videoUriResponse.getTaskId(), "PENDING");
                videoUriTask.setCourse(uploadTask.getCourse());
                taskRepository.save(videoUriTask);
                logger.info("Created video URI task: " + videoUriResponse.getTaskId());
            }
        }
    } catch (Exception e) {
        logger.error("Error initiating video URI request for: " + uploadTask.getTaskId(), e);
    }
}
    
    private void updateCourseWithCollectionName(Long courseId, TaskResponseDto parseTaskStatus) {
        try {
            if (parseTaskStatus.getResult() != null) {
                // Check if result has qdrantCollectionName
                Object result = parseTaskStatus.getResult();
                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    String collectionName = (String) resultMap.get("qdrant_collection_name");
                    
                    if (collectionName != null) {
                        // Determine if this is a PDF or DOCX based on the original file
                        String fileName = getFileName(parseTaskStatus);
                        
                        if (fileName != null) {
                            if (fileName.toLowerCase().endsWith(".pdf")) {
                                courseService.setPdfCollectionName(courseId, collectionName);
                                logger.info("Updated course " + courseId + " with PDF collection: " + collectionName);
                            } else if (fileName.toLowerCase().endsWith(".docx")) {
                                courseService.setDocxCollectionName(courseId, collectionName);
                                logger.info("Updated course " + courseId + " with DOCX collection: " + collectionName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error updating course with collection name", e);
        }
    }
    
    private void updateCourseWithVideoUri(Task videoUriTask) {
        try {
            TaskResponseDto taskStatus = apiClient.getTaskStatus(videoUriTask.getTaskId()).block();
            
            if (taskStatus != null && taskStatus.getResult() != null) {
                Object result = taskStatus.getResult();
                if (result instanceof Map) {
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    Object videoObj = resultMap.get("video");
                    
                    if (videoObj instanceof Map) {
                        Map<String, Object> videoMap = (Map<String, Object>) videoObj;
                        
                        // Convert the map to a Video object
                        com.example.demo.model.Video video = new com.example.demo.model.Video();
                        video.setType((String) videoMap.get("type"));
                        video.setUri((String) videoMap.get("uri"));
                        video.setPath((String) videoMap.get("path"));
                        
                        // Handle URI data if present
                        Object uriDataObj = videoMap.get("uri_data");
                        if (uriDataObj instanceof Map) {
                            // Convert and set URI data
                            Map<String, String> uriData = (Map<String, String>) uriDataObj;
                            video.setUriData(uriData);
                        }
                        
                        // Handle duration if present
                        Object durationObj = videoMap.get("duration");
                        if (durationObj instanceof Map) {
                            Map<String, Integer> durationMap = (Map<String, Integer>) durationObj;
                            com.example.demo.model.Duration duration = new com.example.demo.model.Duration(
                                    durationMap.get("minutes"),
                                    durationMap.get("seconds")
                            );
                            video.setDuration(duration);
                        }
                        
                        // Update the course with the video
                        courseService.setVideo(videoUriTask.getCourse().getId(), video);
                        logger.info("Updated course " + videoUriTask.getCourse().getId() + " with video URI");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error updating course with video URI", e);
        }
    }
    
    private UploadResponseDto parseUploadResponse(String jsonResult) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonResult, UploadResponseDto.class);
        } catch (Exception e) {
            logger.error("Error parsing upload response", e);
            return null;
        }
    }
    
    private String getFileName(TaskResponseDto taskStatus) {
        try {
            if (taskStatus.getResult() instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) taskStatus.getResult();
                return (String) resultMap.get("filename");
            }
        } catch (Exception e) {
            logger.error("Error getting filename", e);
        }
        return null;
    }

    
}