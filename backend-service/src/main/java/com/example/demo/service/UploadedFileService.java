package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Course;
import com.example.demo.model.UploadedFile;
import com.example.demo.repository.UploadedFileRepository;

@Service
public class UploadedFileService {

	private final UploadedFileRepository repository;

	@Autowired
	public UploadedFileService(UploadedFileRepository repository) {
		this.repository = repository;
	}

	public UploadedFile save(UploadedFile uf) { return repository.save(uf); }

	public List<UploadedFile> findByCourse(Course course) { return repository.findByCourse(course); }

	public List<UploadedFile> findByCourseId(Long courseId) { return repository.findByCourseId(courseId); }

	public Optional<UploadedFile> findById(String fileId) { return repository.findById(fileId); }

	@Transactional
	public void deleteByFileId(String fileId) {
		repository.deleteByFileId(fileId);
	}

	@Transactional
	public void deleteByCourseId(Long courseId) {
		repository.deleteByCourseId(courseId);
	}
}
