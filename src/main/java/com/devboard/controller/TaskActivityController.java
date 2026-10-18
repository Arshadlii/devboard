package com.devboard.controller;

import com.devboard.dto.TaskActivityResponse;
import com.devboard.service.TaskActivityService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
    "/api/projects/{projectId}/tasks/{taskId}/activities"
)
public class TaskActivityController {

    private final TaskActivityService activityService;

    public TaskActivityController(
            TaskActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ResponseEntity<List<TaskActivityResponse>> getActivities(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            Authentication authentication) {

        return ResponseEntity.ok(
            activityService.getTaskActivities(
                projectId,
                taskId,
                authentication.getName()
            )
        );
    }
}