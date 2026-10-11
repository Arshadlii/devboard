package com.devboard.dto;

import com.devboard.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;

public record CreateProjectRequest(

    @NotBlank
    @Size(max = 150)
    String name,

    @Size(max = 2000)
    String description,

    LocalDate startDate,

    @NotNull
    @FutureOrPresent
    LocalDate deadline,

    @NotNull
    Priority priority
) {}