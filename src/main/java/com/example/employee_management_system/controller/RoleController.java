package com.example.employee_management_system.controller;

import com.example.employee_management_system.dto.RoleDto;
import com.example.employee_management_system.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('Admin')")   // ← class-level: ALL role endpoints → Admin only
public class RoleController {

	private final RoleService roleService;

	@PostMapping
	public ResponseEntity<RoleDto> create(@Valid @RequestBody RoleDto dto) {
		RoleDto created = roleService.create(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping
	public List<RoleDto> getAll() {
		return roleService.getAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<RoleDto> getById(@PathVariable Long id) {
		return ResponseEntity.ok(roleService.getById(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<RoleDto> update(@PathVariable Long id, @Valid @RequestBody RoleDto dto) {
		return ResponseEntity.ok(roleService.update(id, dto));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		roleService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
