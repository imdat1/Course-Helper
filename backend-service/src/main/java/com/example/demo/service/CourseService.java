package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.client.ApiClient;
import com.example.demo.dto.TaskResponseDto;
import com.example.demo.model.Course;
import com.example.demo.model.FlashCard;
import com.example.demo.model.User;
import com.example.demo.model.Video;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.TaskRepository;

@Service
public class CourseService {
    
    private final CourseRepository courseRepository;
    private final ApiClient apiClient;
    private final UploadedFileService uploadedFileService;
    private final TaskRepository taskRepository;

    public CourseService(CourseRepository courseRepository,
                         ApiClient apiClient,
                         UploadedFileService uploadedFileService,
                         TaskRepository taskRepository) {
        this.courseRepository = courseRepository;
        this.apiClient = apiClient;
        this.uploadedFileService = uploadedFileService;
        this.taskRepository = taskRepository;
    }
    
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
    
    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }
    
    public List<Course> getCoursesByOwner(User owner) {
        return courseRepository.findByOwner(owner);
    }
    
    public Course createCourse(Course course) {
        return courseRepository.save(course);
    }
    
    public Course updateCourse(Course course) {
        return courseRepository.save(course);
    }
    
    @Transactional
    public void deleteCourse(Long id) {
        // Fetch course to obtain collection names before deletion
        Optional<Course> maybeCourse = courseRepository.findById(id);
        if (!maybeCourse.isPresent()) {
            return;
        }
        Course course = maybeCourse.get();

        // Attempt to delete entire collections (PDF, DOCX, PPTX) if present
        try {
            taskRepository.deleteByCourseId(id);
        } catch (Exception e) {
            System.err.println("Failed to cleanup tasks: " + e.getMessage());
        }
        boolean attemptedDeletion = false;
        boolean allDeleted = true;
        java.util.Set<String> collections = new java.util.HashSet<>();
        if (course.getPdfCollectionName() != null) collections.add(course.getPdfCollectionName());
        if (course.getDocxCollectionName() != null) collections.add(course.getDocxCollectionName());
        if (course.getPptxCollectionName() != null) collections.add(course.getPptxCollectionName());

        for (String coll : collections) {
            try {
                attemptedDeletion = true;
                Map<String, Object> resp = apiClient.deleteCollection(coll).block();
                String taskId = resp != null && resp.get("task_id") instanceof String ? (String) resp.get("task_id") : null;
                if (taskId != null) {
                    var status = waitForTaskCompletion(taskId, 24, 5_000);
                    boolean deleted = status != null && "SUCCESS".equals(status.getStatus());
                    if (!deleted) allDeleted = false;
                }
            } catch (Exception e) {
                System.err.println("Failed to enqueue/poll collection deletion (" + coll + "): " + e.getMessage());
                allDeleted = false;
            }
        }

        if (attemptedDeletion && !allDeleted) {
            throw new RuntimeException("Collection deletion failed; aborting course deletion");
        }

        // Cleanup uploaded file metadata for this course
        uploadedFileService.deleteByCourseId(id);

        // Finally delete the course (will cascade flashcards/messages/video)
        courseRepository.deleteById(id);
    }

    private TaskResponseDto waitForTaskCompletion(String taskId, int maxAttempts, long intervalMs) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                TaskResponseDto taskStatus = apiClient.getTaskStatus(taskId).block();
                if (taskStatus != null && ("SUCCESS".equals(taskStatus.getStatus()) || "FAILURE".equals(taskStatus.getStatus()))) {
                    return taskStatus;
                }
                Thread.sleep(intervalMs);
            } catch (Exception e) {
                System.err.println("Error polling task status: " + e.getMessage());
            }
        }
        return null;
    }
    
    @Transactional
    public Course setPdfCollectionName(Long courseId, String collectionName) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setPdfCollectionName(collectionName);
        return courseRepository.save(course);
    }
    
    @Transactional
    public Course setDocxCollectionName(Long courseId, String collectionName) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setDocxCollectionName(collectionName);
        return courseRepository.save(course);
    }

    @Transactional
    public Course setPptxCollectionName(Long courseId, String collectionName) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setPptxCollectionName(collectionName);
        return courseRepository.save(course);
    }
    
    @Transactional
    public Course setVideo(Long courseId, Video video) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setVideo(video);
        return courseRepository.save(course);
    }

    @Transactional
    public Course clearVideo(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setVideo(null);
        return courseRepository.save(course);
    }
    
    @Transactional
    public Course addFlashCard(Long courseId, FlashCard flashCard) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.addFlashCard(flashCard);
        return courseRepository.save(course);
    }
}