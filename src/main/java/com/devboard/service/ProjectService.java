package com.devboard.service;

import com.devboard.dto.CreateProjectRequest;
import com.devboard.dto.ProjectResponse;
import com.devboard.entity.Project;
import com.devboard.entity.ProjectMember;
import com.devboard.entity.ProjectStatus;
import com.devboard.entity.Role;
import com.devboard.entity.User;
import com.devboard.repository.ProjectMemberRepository;
import com.devboard.repository.ProjectRepository;
import com.devboard.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectResponse createProject(
            CreateProjectRequest request,
            String authenticatedEmail) {

        User creator = findActiveUser(authenticatedEmail);

        if (creator.getRole() != Role.PROJECT_MANAGER
                && creator.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "Only project managers and administrators can create projects.");
        }

        if (request.startDate() != null
                && request.deadline().isBefore(request.startDate())) {
            throw new IllegalArgumentException(
                    "Deadline cannot be before the start date.");
        }

        Project project = new Project();
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setDeadline(request.deadline());
        project.setPriority(request.priority());
        project.setStatus(ProjectStatus.PLANNING);
        project.setCreatedBy(creator);

        Project savedProject = projectRepository.save(project);

        // The creator is automatically a project member.
        memberRepository.save(new ProjectMember(savedProject, creator));

        return ProjectResponse.from(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(String authenticatedEmail) {

        User user = findActiveUser(authenticatedEmail);

        if (user.getRole() == Role.ADMIN) {
            return projectRepository.findAll()
                    .stream()
                    .map(ProjectResponse::from)
                    .toList();
        }

        // Include projects created by the user and projects
        // where the user is a member.
        var projects = new LinkedHashMap<Long, Project>();

        projectRepository
                .findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(user.getEmail())
                .forEach(project -> projects.put(project.getId(), project));

        memberRepository.findByUserEmailIgnoreCase(user.getEmail())
                .forEach(member -> projects.put(
                        member.getProject().getId(),
                        member.getProject()));

        return projects.values().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(
            Long projectId,
            String authenticatedEmail) {

        User user = findActiveUser(authenticatedEmail);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found."));

        boolean allowed = user.getRole() == Role.ADMIN
                || project.getCreatedBy().getId().equals(user.getId())
                || memberRepository.existsByProjectIdAndUserId(
                        projectId, user.getId());

        if (!allowed) {
            // Do not reveal another user's project to unauthorized users.
            throw new AccessDeniedException(
                    "You do not have access to this project.");
        }

        return ProjectResponse.from(project);
    }

    private User findActiveUser(String email) {
        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));

        if (!user.isActive()) {
            throw new AccessDeniedException("This account is inactive.");
        }

        return user;
    }
}