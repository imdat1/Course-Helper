package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Course;
import com.example.demo.model.Task;
import com.example.demo.model.User;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByUser(User user);
    List<Task> findByCourse(Course course);
    List<Task> findByStatus(String status);

    @Modifying
    @Transactional
    @Query("delete from Task t where t.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}