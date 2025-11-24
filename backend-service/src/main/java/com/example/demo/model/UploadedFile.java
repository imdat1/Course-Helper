package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile {
    @Id
    @Column(name = "file_id")
    private String fileId;

    private String filename;

    private String type; // PDF, DOCX, etc.

    private String collectionName;

    // Processing status for UX: PENDING, READY, FAILED
    @Column(name = "status")
    private String status;

    // The current background processing task id for this file (Celery task id)
    @Column(name = "processing_task_id")
    private String processingTaskId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProcessingTaskId() { return processingTaskId; }
    public void setProcessingTaskId(String processingTaskId) { this.processingTaskId = processingTaskId; }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
    }
}