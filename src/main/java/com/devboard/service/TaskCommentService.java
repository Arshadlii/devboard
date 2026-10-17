package com.devboard.service;

import com.devboard.dto.CommentResponse;
import com.devboard.dto.CreateCommentRequest;
import com.devboard.entity.*;
import com.devboard.repository.*;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;

@Service
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository memberRepository;

    public TaskCommentService(
            TaskCommentRepository commentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectMemberRepository memberRepository) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public CommentResponse addComment(
            Long projectId,
            Long taskId,
            CreateCommentRequest request,
            String authenticatedEmail) {

        User actor = findActiveUser(authenticatedEmail);
        Task task = findTaskInProject(projectId, taskId);

        checkProjectAccess(task, projectId, actor);

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setAuthor(actor);
        comment.setContent(request.content().trim());

        if (comment.getContent().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Comment cannot be empty"
            );
        }

        return CommentResponse.from(
            commentRepository.save(comment)
        );
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(
            Long projectId,
            Long taskId,
            String authenticatedEmail) {

        User actor = findActiveUser(authenticatedEmail);
        Task task = findTaskInProject(projectId, taskId);

        checkProjectAccess(task, projectId, actor);

        return commentRepository
            .findByTaskIdOrderByCreatedAtAsc(taskId)
            .stream()
            .map(CommentResponse::from)
            .toList();
    }

    private Task findTaskInProject(
            Long projectId, Long taskId) {

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found"
            ));

        if (!task.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found in this project"
            );
        }

        return task;
    }

    private void checkProjectAccess(
            Task task, Long projectId, User actor) {

        boolean isAdmin = actor.getRole() == Role.ADMIN;

        boolean isCreator =
            task.getProject().getCreatedBy().getId()
                .equals(actor.getId());

        boolean isMember =
            memberRepository.existsByProjectIdAndUserId(
                projectId, actor.getId()
            );

        if (!isAdmin && !isCreator && !isMember) {
            throw new AccessDeniedException(
                "You do not have access to this project's comments."
            );
        }
    }

    private User findActiveUser(String email) {
        String normalizedEmail =
            email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository
            .findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "User account not found"
            ));

        if (!user.isActive()) {
            throw new AccessDeniedException(
                "This account is inactive."
            );
        }

        return user;
    }
}