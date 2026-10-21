
package com.devboard.dto;

import com.devboard.entity.TaskAttachment;
import java.time.LocalDateTime;

public record AttachmentResponse(
    Long id,
    Long taskId,
    String originalFilename,
    String contentType,
    long fileSize,
    String uploadedBy,
    LocalDateTime createdAt
) {
    public static AttachmentResponse from(TaskAttachment attachment) {
        return new AttachmentResponse(
            attachment.getId(),
            attachment.getTask().getId(),
            attachment.getOriginalFilename(),
            attachment.getContentType(),
            attachment.getFileSize(),
            attachment.getUploadedBy().getName(),
            attachment.getCreatedAt()
        );
    }
}
