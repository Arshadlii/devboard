package com.devboard.dto;

import com.devboard.entity.ProjectMember;
import java.time.LocalDateTime;

public record ProjectMemberResponse(
    Long userId,
    String name,
    String email,
    String role,
    LocalDateTime joinedAt
) {
    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
            member.getUser().getId(),
            member.getUser().getName(),
            member.getUser().getEmail(),
            member.getUser().getRole().name(),
            member.getJoinedAt()
        );
    }
}