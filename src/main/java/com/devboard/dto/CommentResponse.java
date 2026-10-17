package com.devboard.dto;

import com.devboard.entity.TaskComment;
import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long taskId,
    Long authorId,
    String authorName,
    String content,
    LocalDateTime createdAt
) {
    public static CommentResponse from(TaskComment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getTask().getId(),
            comment.getAuthor().getId(),
            comment.getAuthor().getName(),
            comment.getContent(),
            comment.getCreatedAt()
        );
    }
}