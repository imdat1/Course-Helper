package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Course;
import com.example.demo.model.UploadedFile;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, String> {
	List<UploadedFile> findByCourse(Course course);
	List<UploadedFile> findByCourseId(Long courseId);
	@Modifying
	@Transactional
	void deleteByFileId(String fileId);

	@Modifying
	@Transactional
	void deleteByCourseId(Long courseId);
}
