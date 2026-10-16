package com.devboard.service;

import com.devboard.dto.CreateTaskRequest;
import com.devboard.dto.TaskResponse;
import com.devboard.entity.Project;
import com.devboard.dto.UpdateTaskStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
// import com.devboard.entity.ProjectMember;
import com.devboard.entity.Role;
import com.devboard.entity.Task;
import com.devboard.entity.TaskStatus;
import com.devboard.entity.User;
import com.devboard.repository.ProjectMemberRepository;
import com.devboard.repository.ProjectRepository;
import com.devboard.repository.TaskRepository;
import com.devboard.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse createTask(
            Long projectId,
            CreateTaskRequest request,
            String authenticatedEmail) {

        User actor = findActiveUser(authenticatedEmail);

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() ->
                new IllegalArgumentException("Project not found.")
            );

        boolean canCreate =
            actor.getRole() == Role.ADMIN
            || (
                actor.getRole() == Role.PROJECT_MANAGER
                && project.getCreatedBy().getId().equals(actor.getId())
            );

        if (!canCreate) {
            throw new AccessDeniedException(
                "Only the project's manager or an administrator can create tasks."
            );
        }

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setProject(project);
        task.setCreatedBy(actor);
        task.setPriority(request.priority());
        task.setStatus(TaskStatus.TODO);
        task.setDeadline(request.deadline());

        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() ->
                    new IllegalArgumentException("Assignee not found.")
                );

            if (!assignee.isActive()) {
                throw new IllegalArgumentException(
                    "Cannot assign a task to an inactive user."
                );
            }

            if (assignee.getRole() != Role.DEVELOPER) {
                throw new IllegalArgumentException(
                    "Tasks can only be assigned to developers."
                );
            }

            boolean isMember =
                memberRepository.existsByProjectIdAndUserId(
                    projectId, assignee.getId()
                );

            if (!isMember) {
                throw new IllegalArgumentException(
                    "The assignee must be a member of this project."
                );
            }

            task.setAssignee(assignee);
        }

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getProjectTasks(
            Long projectId,
            String authenticatedEmail) {

        User actor = findActiveUser(authenticatedEmail);

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() ->
                new IllegalArgumentException("Project not found.")
            );

        boolean canView =
            actor.getRole() == Role.ADMIN
            || project.getCreatedBy().getId().equals(actor.getId())
            || memberRepository.existsByProjectIdAndUserId(
                projectId, actor.getId()
            );

        if (!canView) {
            throw new AccessDeniedException(
                "You do not have access to this project's tasks."
            );
        }

        return taskRepository
            .findByProjectIdOrderByCreatedAtDesc(projectId)
            .stream()
            .map(TaskResponse::from)
            .toList();
    }

    private User findActiveUser(String email) {
        String normalizedEmail = email.trim()
            .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() ->
                new IllegalArgumentException("User account not found.")
            );

        if (!user.isActive()) {
            throw new AccessDeniedException("This account is inactive.");
        }

        return user;
    }
    @Transactional
    public TaskResponse updateTaskStatus(
        Long projectId,
        Long taskId,
        UpdateTaskStatusRequest request,
        String authenticatedEmail)  {

    User actor = findActiveUser(authenticatedEmail);

    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Task not found"
        ));

     projectId = task.getProject().getId();

    boolean isAdmin = actor.getRole() == Role.ADMIN;

    boolean isProjectManager =
        actor.getRole() == Role.PROJECT_MANAGER
        && task.getProject().getCreatedBy().getId()
            .equals(actor.getId());

    boolean isAssignee =
        task.getAssignee() != null
        && task.getAssignee().getId().equals(actor.getId());

    boolean isProjectMember =
        memberRepository.existsByProjectIdAndUserId(
            projectId, actor.getId()
        );

    if (!isAdmin && !isProjectManager
            && !(isAssignee && isProjectMember)) {
        throw new org.springframework.security.access.AccessDeniedException(
            "You cannot update this task."
        );
    }
    if (!task.getProject().getId().equals(projectId)) {
    throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Task not found in this project"
    );
}

    TaskStatus current = task.getStatus();
    TaskStatus target = request.status();

    if (current == target) {
        return TaskResponse.from(task);
    }

    boolean allowedTransition = switch (current) {
        case TODO ->
            target == TaskStatus.IN_PROGRESS;

        case IN_PROGRESS ->
            target == TaskStatus.TODO
            || target == TaskStatus.IN_REVIEW;

        case IN_REVIEW ->
            target == TaskStatus.IN_PROGRESS
            || (target == TaskStatus.COMPLETED
                && (isAdmin || isProjectManager));

        case COMPLETED -> false;
    };

    if (!allowedTransition) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid task status transition: "
                + current + " -> " + target
        );
    }

    task.setStatus(target);

    return TaskResponse.from(taskRepository.save(task));
}
}