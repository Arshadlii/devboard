package com.devboard.dto;

import com.devboard.entity.Priority;
import com.devboard.entity.Project;
import com.devboard.entity.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    LocalDate startDate,
    LocalDate deadline,
    Priority priority,
    ProjectStatus status,
    Long createdById,
    String createdByName,
    LocalDateTime createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getStartDate(),
            project.getDeadline(),
            project.getPriority(),
            project.getStatus(),
            project.getCreatedBy().getId(),
            project.getCreatedBy().getName(),
            project.getCreatedAt()
        );
    }
}