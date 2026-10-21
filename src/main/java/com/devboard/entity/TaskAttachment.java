
package com.devboard.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_attachments")
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, unique = true, length = 36)
    private String storageFilename;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TaskAttachment() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public User getUploadedBy() { return uploadedBy; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageFilename() { return storageFilename; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setTask(Task task) { this.task = task; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    public void setStorageFilename(String storageFilename) {
        this.storageFilename = storageFilename;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}
