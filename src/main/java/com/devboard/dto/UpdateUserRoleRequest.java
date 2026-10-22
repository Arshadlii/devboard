package com.devboard.dto;

import com.devboard.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull Role role
) {}