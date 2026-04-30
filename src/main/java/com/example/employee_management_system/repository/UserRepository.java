package com.example.employee_management_system.repository;

import com.example.employee_management_system.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesById(Long id);

    // ✅ Same as above but by username — used during login to load roles
    //    without a second DB query (avoids LazyInitializationException)
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByUsername(String username);
}
