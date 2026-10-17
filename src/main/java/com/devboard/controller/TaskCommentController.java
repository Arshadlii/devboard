package com.devboard.controller;

import com.devboard.dto.CommentResponse;
import com.devboard.dto.CreateCommentRequest;
import com.devboard.service.TaskCommentService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
    "/api/projects/{projectId}/tasks/{taskId}/comments"
)
public class TaskCommentController {

    private final TaskCommentService commentService;

    public TaskCommentController(
            TaskCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {

        CommentResponse response = commentService.addComment(
            projectId,
            taskId,
            request,
            authentication.getName()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            Authentication authentication) {

        return ResponseEntity.ok(
            commentService.getComments(
                projectId,
                taskId,
                authentication.getName()
            )
        );
    }
}