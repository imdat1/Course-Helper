package com.example.demo.dto;

import lombok.Data;

@Data
public class UploadedFileDto {
	private String fileId;
	private String filename;
	private String type; // PDF, DOCX
	private String collectionName;
	private String status; // PENDING, READY, FAILED
	private String processingTaskId; // Celery task id for polling
}
