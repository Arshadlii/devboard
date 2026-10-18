package com.devboard.repository;

import com.devboard.entity.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskActivityRepository
        extends JpaRepository<TaskActivity, Long> {

    List<TaskActivity> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}