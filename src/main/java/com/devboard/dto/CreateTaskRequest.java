package com.devboard.dto;

import com.devboard.entity.Priority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(

    @NotBlank
    @Size(max = 200)
    String title,

    @Size(max = 3000)
    String description,

    @NotNull
    Priority priority,

    @FutureOrPresent
    LocalDate deadline,

    Long assigneeId
) {}