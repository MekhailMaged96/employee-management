package com.example.employee_management_system.controller;

import com.example.employee_management_system.dto.UserDto;
import com.example.employee_management_system.service.AuthService;
import com.example.employee_management_system.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final AuthService authService;

	@GetMapping
	public List<UserDto> getAllUsers() {
		return authService.getAllUsers();
	}

	@PostMapping
	public ResponseEntity<UserDto> createUser(@Valid @RequestBody RegisterRequest request) {
		UserDto created = authService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}


}


