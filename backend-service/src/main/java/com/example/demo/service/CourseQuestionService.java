package com.example.demo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.model.Course;
import com.example.demo.model.CourseQuestion;
import com.example.demo.repository.CourseQuestionRepository;

@Service
public class CourseQuestionService {
    private final CourseQuestionRepository repository;

    public CourseQuestionService(CourseQuestionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CourseQuestion save(CourseQuestion q) { return repository.save(q); }

    public List<CourseQuestion> findByCourse(Course course) { return repository.findByCourse(course); }
    public List<CourseQuestion> findByFileId(String fileId) { return repository.findByFileId(fileId); }
    public Optional<CourseQuestion> findById(String id) { return repository.findById(id); }
    @Transactional
    public void deleteByFileId(String fileId) { repository.deleteByFileId(fileId); }
}