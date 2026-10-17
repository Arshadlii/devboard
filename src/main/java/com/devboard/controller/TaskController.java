package com.devboard.controller;

import com.devboard.dto.CreateTaskRequest;
import com.devboard.dto.TaskResponse;
import com.devboard.service.TaskService;
import com.devboard.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

@PatchMapping("/{taskId}/status")
public ResponseEntity<TaskResponse> updateTaskStatus(
        @PathVariable Long projectId,
        @PathVariable Long taskId,
        @Valid @RequestBody UpdateTaskStatusRequest request,
        Authentication authentication) {

    TaskResponse response = taskService.updateTaskStatus(
        projectId,
        taskId,
        request,
        authentication.getName()
    );

    return ResponseEntity.ok(response);
}

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {

        TaskResponse response = taskService.createTask(
            projectId,
            request,
            authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getProjectTasks(
            @PathVariable Long projectId,
            Authentication authentication) {

        return ResponseEntity.ok(
            taskService.getProjectTasks(
                projectId,
                authentication.getName()
            )
        );
    }
    
}