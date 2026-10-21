
package com.devboard.controller;

import com.devboard.dto.AttachmentResponse;
import com.devboard.service.TaskAttachmentService;
import com.devboard.service.TaskAttachmentService.DownloadedAttachment;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(
    "/api/projects/{projectId}/tasks/{taskId}/attachments"
)
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;

    public TaskAttachmentController(
            TaskAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        AttachmentResponse response = attachmentService.upload(
            projectId,
            taskId,
            file,
            authentication.getName()
        );

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AttachmentResponse>> list(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            Authentication authentication) {

        return ResponseEntity.ok(
            attachmentService.list(
                projectId,
                taskId,
                authentication.getName()
            )
        );
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long attachmentId,
            Authentication authentication) {

        DownloadedAttachment file = attachmentService.download(
            projectId,
            taskId,
            attachmentId,
            authentication.getName()
        );

        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(file.filename(), StandardCharsets.UTF_8)
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentLength(file.content().length)
            .header("X-Content-Type-Options", "nosniff")
            .body(new ByteArrayResource(file.content()));
    }
}
