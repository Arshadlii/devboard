
package com.devboard.controller;

import com.devboard.dto.UpdateUserRoleRequest;
import com.devboard.dto.UpdateUserStatusRequest;
import com.devboard.dto.UserResponse;
import com.devboard.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<UserResponse> getUsers() {
        return adminService.getAllUsers();
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {

        return adminService.updateRole(
                id, request.role(), authentication.getName()
        );
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {

        return adminService.updateStatus(
                id, request.active(), authentication.getName()
        );
    }
}
