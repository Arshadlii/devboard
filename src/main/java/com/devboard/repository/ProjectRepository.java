package com.devboard.repository;

import com.devboard.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    List<Project> findByCreatedByEmailIgnoreCaseOrderByCreatedAtDesc(
        String email
    );
}