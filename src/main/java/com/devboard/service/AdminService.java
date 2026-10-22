
package com.devboard.service;

import com.devboard.dto.UserResponse;
import com.devboard.entity.Role;
import com.devboard.entity.User;
import com.devboard.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateRole(
            Long userId, Role newRole, String currentEmail) {

        User user = findUser(userId);

        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot change your own role."
            );
        }

        if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .count();

            if (adminCount <= 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "You cannot remove the last Admin."
                );
            }
        }

        user.setRole(newRole);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateStatus(
            Long userId, boolean active, String currentEmail) {

        User user = findUser(userId);

        if (user.getEmail().equalsIgnoreCase(currentEmail) && !active) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot deactivate your own account."
            );
        }

        if (user.getRole() == Role.ADMIN && !active) {
            long activeAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN && u.isActive())
                    .count();

            if (activeAdminCount <= 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "You cannot deactivate the last active Admin."
                );
            }
        }

        user.setActive(active);
        return UserResponse.from(userRepository.save(user));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found."
                ));
    }
}
