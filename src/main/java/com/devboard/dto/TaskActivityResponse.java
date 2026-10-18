package com.devboard.dto;

import com.devboard.entity.TaskActivity;
import java.time.LocalDateTime;

public record TaskActivityResponse(
    Long id,
    Long projectId,
    Long taskId,
    Long actorId,
    String actorName,
    String action,
    String details,
    LocalDateTime createdAt
) {
    public static TaskActivityResponse from(TaskActivity activity) {
        return new TaskActivityResponse(
            activity.getId(),
            activity.getProject().getId(),
            activity.getTask().getId(),
            activity.getActor().getId(),
            activity.getActor().getName(),
            activity.getAction(),
            activity.getDetails(),
            activity.getCreatedAt()
        );
    }
}