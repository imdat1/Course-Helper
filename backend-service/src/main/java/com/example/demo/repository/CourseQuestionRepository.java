package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.model.CourseQuestion;
import com.example.demo.model.Course;

public interface CourseQuestionRepository extends JpaRepository<CourseQuestion, String> {
    List<CourseQuestion> findByCourse(Course course);
    List<CourseQuestion> findByFileId(String fileId);

    @Modifying
    @Transactional
    void deleteByFileId(String fileId);
}