
package com.devboard.repository;

import com.devboard.entity.Role;
import com.devboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        SELECT u FROM User u
        WHERE u.active = true
          AND u.role = :role
          AND (
              LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%'))
              OR LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%'))
          )
        ORDER BY u.name
        """)
    List<User> searchActiveUsersByRole(
            @Param("role") Role role,
            @Param("term") String term
    );
}
