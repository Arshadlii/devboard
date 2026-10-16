package com.devboard.dto;

import com.devboard.entity.Task;
import com.devboard.entity.TaskStatus;
import com.devboard.entity.Priority;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
    Long id,
    String title,
    String description,
    Long projectId,
    Long createdById,
    Long assigneeId,
    String assigneeName,
    Priority priority,
    TaskStatus status,
    LocalDate deadline,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getProject().getId(),
            task.getCreatedBy().getId(),
            task.getAssignee() == null
                ? null : task.getAssignee().getId(),
            task.getAssignee() == null
                ? null : task.getAssignee().getName(),
            task.getPriority(),
            task.getStatus(),
            task.getDeadline(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}