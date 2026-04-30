package com.example.employee_management_system.controller;

import com.example.employee_management_system.dto.UserDto;
import com.example.employee_management_system.service.AuthService;
import com.example.employee_management_system.dto.auth.RegisterRequest;
import com.example.employee_management_system.security.TokenUserExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final AuthService authService;
	private final TokenUserExtractor tokenUserExtractor;  // ✅ injected to read current user from context

	// ──────────────────────────────────────────────────────────────
	//  GET /api/users/me
	//  Returns the profile of the currently authenticated user.
	//
	//  How it works:
	//    1. JWT filter runs → populates SecurityContext with the user
	//    2. TokenUserExtractor reads user id from SecurityContext
	//       (Pattern 1 — no DB call, no token re-parsing)
	//    3. We fetch the full profile from DB using that id
	// ──────────────────────────────────────────────────────────────
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")  // any logged-in user can access their own profile
	public ResponseEntity<UserDto> getMyProfile() {
		Long userId = tokenUserExtractor.getUserIdFromContext();  // ← from SecurityContextHolder
		UserDto profile = authService.getUserById(userId);
		return ResponseEntity.ok(profile);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('Admin')")
	public List<UserDto> getAllUsers() {
		return authService.getAllUsers();
	}

	@PostMapping
	@PreAuthorize("hasAuthority('Admin')")
	public ResponseEntity<UserDto> createUser(@Valid @RequestBody RegisterRequest request) {
		UserDto created = authService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// assign multiple roles by ids
    @PostMapping("/{userId}/roles/assign")
	@PreAuthorize("hasAuthority('Admin')")
	public ResponseEntity<UserDto> assignRoles(@PathVariable Long userId, @RequestBody Set<Long> roleIds) {
		UserDto updated = authService.assignRolesToUser(userId, roleIds);
		return ResponseEntity.ok(updated);
	}

	// unassign multiple roles by ids
    @PostMapping("/{userId}/roles/unassign")
	@PreAuthorize("hasAuthority('Admin')")
	public ResponseEntity<UserDto> unassignRoles(@PathVariable Long userId, @RequestBody Set<Long> roleIds) {
		UserDto updated = authService.unassignRolesFromUser(userId, roleIds);
		return ResponseEntity.ok(updated);
	}


}
