package com.devboard.dto;

import com.devboard.entity.Role;
import com.devboard.entity.User;

public record UserResponse(
    Long id,
    String name,
    String email,
    Role role,
    boolean active
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive()
        );
    }
}