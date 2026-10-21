
package com.devboard.service;

import com.devboard.dto.AttachmentResponse;
import com.devboard.entity.*;
import com.devboard.repository.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TaskAttachmentService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final List<String> ALLOWED_EXTENSIONS =
        List.of("pdf", "png", "jpg", "jpeg", "txt", "csv", "doc", "docx");

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository memberRepository;
    private final TaskActivityRepository activityRepository;
    private final Path uploadDirectory;

    public TaskAttachmentService(
            TaskAttachmentRepository attachmentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectMemberRepository memberRepository,
            TaskActivityRepository activityRepository,
            @Value("${devboard.upload-dir:uploads}") String uploadDir) {

        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.activityRepository = activityRepository;

        this.uploadDirectory = Paths.get(uploadDir)
            .toAbsolutePath()
            .normalize();
    }

    @Transactional
    public AttachmentResponse upload(
            Long projectId,
            Long taskId,
            MultipartFile file,
            String authenticatedEmail) {

        User actor = findActiveUser(authenticatedEmail);
        Task task = findAuthorizedTask(projectId, taskId, actor);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Please select a non-empty file."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Maximum file size is 5 MB."
            );
        }

        String originalName =
            StringUtils.cleanPath(
                file.getOriginalFilename() == null
                    ? ""
                    : file.getOriginalFilename()
            );

        if (originalName.isBlank()
                || originalName.contains("..")
                || originalName.contains("/")
                || originalName.contains("\\")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid filename."
            );
        }

        String extension = StringUtils.getFilenameExtension(originalName);

        if (extension == null
                || !ALLOWED_EXTENSIONS.contains(
                    extension.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Allowed file types: PDF, PNG, JPG, TXT, CSV, DOC and DOCX."
            );
        }

        String storageName = UUID.randomUUID().toString();
        Path destination = uploadDirectory.resolve(storageName).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid file path."
            );
        }

        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), destination);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not store uploaded file."
            );
        }

        try {
            TaskAttachment attachment = new TaskAttachment();
            attachment.setTask(task);
            attachment.setUploadedBy(actor);
            attachment.setOriginalFilename(originalName);
            attachment.setStorageFilename(storageName);
            attachment.setContentType(
                contentTypeForExtension(extension)
            );
            attachment.setFileSize(file.getSize());

            TaskAttachment saved =
                attachmentRepository.saveAndFlush(attachment);

            TaskActivity activity = new TaskActivity();
            activity.setProject(task.getProject());
            activity.setTask(task);
            activity.setActor(actor);
            activity.setAction("ATTACHMENT_UPLOADED");
            activity.setDetails("Uploaded attachment: " + originalName);
            activityRepository.save(activity);

            return AttachmentResponse.from(saved);

        } catch (RuntimeException ex) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException ignored) {
                // Preserve the original database error.
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(
            Long projectId,
            Long taskId,
            String authenticatedEmail) {

        User actor = findActiveUser(authenticatedEmail);
        findAuthorizedTask(projectId, taskId, actor);

        return attachmentRepository
            .findByTaskIdOrderByCreatedAtDesc(taskId)
            .stream()
            .map(AttachmentResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment download(
            Long projectId,
            Long taskId,
            Long attachmentId,
            String authenticatedEmail) {

        User actor = findActiveUser(authenticatedEmail);
        findAuthorizedTask(projectId, taskId, actor);

        TaskAttachment attachment =
            attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Attachment not found."
                ));

        if (!attachment.getTask().getId().equals(taskId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Attachment not found for this task."
            );
        }

        Path path = uploadDirectory
            .resolve(attachment.getStorageFilename())
            .normalize();

        if (!path.startsWith(uploadDirectory) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Attachment file not found."
            );
        }

        try {
            return new DownloadedAttachment(
                Files.readAllBytes(path),
                attachment.getOriginalFilename(),
                attachment.getContentType()
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not read attachment."
            );
        }
    }

    private Task findAuthorizedTask(
            Long projectId,
            Long taskId,
            User actor) {

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Task not found."
            ));

        if (!task.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Task not found in this project."
            );
        }

        boolean allowed =
            actor.getRole() == Role.ADMIN
            || task.getProject().getCreatedBy().getId()
                .equals(actor.getId())
            || memberRepository.existsByProjectIdAndUserId(
                projectId, actor.getId()
            );

        if (!allowed) {
            throw new AccessDeniedException(
                "You do not have access to this task's attachments."
            );
        }

        return task;
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(
            email.trim().toLowerCase(Locale.ROOT)
        ).orElseThrow(() -> new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "User account not found."
        ));

        if (!user.isActive()) {
            throw new AccessDeniedException("This account is inactive.");
        }

        return user;
    }

    private String contentTypeForExtension(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "doc" -> "application/msword";
            case "docx" ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    public record DownloadedAttachment(
        byte[] content,
        String filename,
        String contentType
    ) {}
}
