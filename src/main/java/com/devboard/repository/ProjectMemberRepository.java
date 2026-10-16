package com.devboard.repository;

import com.devboard.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByUserEmailIgnoreCase(String email);

    List<ProjectMember> findByProjectId(Long projectId);

    boolean existsByProjectIdAndUserEmailIgnoreCase(
        Long projectId,
        String email
    );
}