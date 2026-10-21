package com.devboard.controller;

import com.devboard.dto.CreateProjectRequest;
import com.devboard.dto.ProjectResponse;
import com.devboard.service.ProjectService;
import com.devboard.dto.AddProjectMemberRequest;
import com.devboard.dto.ProjectMemberResponse;
import jakarta.validation.Valid;


import com.devboard.dto.DeveloperOptionResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }


    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            Authentication authentication) {

        ProjectResponse response = projectService.createProject(
                request,
                authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects(
            Authentication authentication) {

        return ResponseEntity.ok(
                projectService.getMyProjects(authentication.getName()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        return ResponseEntity.ok(
                projectService.getProject(
                        projectId,
                        authentication.getName()));
    }
    @PostMapping("/{projectId}/members")
public ResponseEntity<ProjectMemberResponse> addMember(
        @PathVariable Long projectId,
        @Valid @RequestBody AddProjectMemberRequest request,
        Authentication authentication) {

    ProjectMemberResponse response = projectService.addMember(
        projectId,
        request,
        authentication.getName()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

@GetMapping("/{projectId}/members")
public ResponseEntity<List<ProjectMemberResponse>> getMembers(
        @PathVariable Long projectId,
        Authentication authentication) {

    return ResponseEntity.ok(
        projectService.getMembers(
            projectId,
            authentication.getName()
        )
    );
}

@GetMapping("/{projectId}/developer-options")
public ResponseEntity<List<DeveloperOptionResponse>> searchDevelopers(
        @PathVariable Long projectId,
        @RequestParam(defaultValue = "") String q,
        Authentication authentication) {

    return ResponseEntity.ok(
            projectService.searchDevelopers(
                    projectId, q, authentication.getName()));
}

@DeleteMapping("/{projectId}/members/{userId}")
public ResponseEntity<Void> removeMember(
        @PathVariable Long projectId,
        @PathVariable Long userId,
        Authentication authentication) {

    projectService.removeMember(
            projectId, userId, authentication.getName());

    return ResponseEntity.noContent().build();
}

}