package com.devboard.service;

import com.devboard.dto.TaskActivityResponse;
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
public class TaskActivityService {

    private final TaskActivityRepository activityRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository memberRepository;

    public TaskActivityService(
            TaskActivityRepository activityRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectMemberRepository memberRepository) {
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskActivityResponse> getTaskActivities(
            Long projectId,
            Long taskId,
            String authenticatedEmail) {

        User actor = userRepository
            .findByEmailIgnoreCase(
                authenticatedEmail.trim().toLowerCase(Locale.ROOT)
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User account not found"
            ));

        if (!actor.isActive()) {
            throw new AccessDeniedException("This account is inactive.");
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Task not found"
            ));

        if (!task.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found in this project"
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
                "You cannot view this task's activity."
            );
        }

        return activityRepository
            .findByTaskIdOrderByCreatedAtDesc(taskId)
            .stream()
            .map(TaskActivityResponse::from)
            .toList();
    }
}