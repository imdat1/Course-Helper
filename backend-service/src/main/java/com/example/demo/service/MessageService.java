package com.example.demo.service;

import com.example.demo.model.Course;
import com.example.demo.model.Message;
import com.example.demo.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final CourseService courseService;
    
    @Autowired
    public MessageService(MessageRepository messageRepository, CourseService courseService) {
        this.messageRepository = messageRepository;
        this.courseService = courseService;
    }
    
    public List<Message> getMessagesByCourse(Long courseId) {
        Course course = courseService.getCourseById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        List<Message> messages = messageRepository.findByCourseOrderByTimestampAsc(course);
        System.out.println("Found " + messages.size() + " messages for course " + courseId);
        
        return messages;
    }
    
    @Transactional
    public Message saveMessage(Message message) {
        // Force timestamp if not already set
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        
        // Verify course relationship is set correctly
        if (message.getCourse() != null && message.getCourse().getId() != null) {
            // Make sure we're using a managed entity for the course
            Course managedCourse = courseService.getCourseById(message.getCourse().getId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            
            // Explicitly set the course and add the message to the course's message list
            message.setCourse(managedCourse);
            managedCourse.addMessage(message);
            
            System.out.println("Saving message with role: " + message.getRole() + 
                              " for course: " + managedCourse.getId());
        } else {
            System.err.println("WARNING: Saving message without course association!");
        }
        
        return messageRepository.save(message);
    }
    
    public Optional<Message> getMessageById(Long id) {
        return messageRepository.findById(id);
    }
    
    @Transactional
    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }
}