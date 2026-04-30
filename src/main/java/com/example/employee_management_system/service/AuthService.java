package com.example.employee_management_system.service;


import com.example.employee_management_system.dto.UserDto;
import com.example.employee_management_system.dto.auth.AuthResponse;
import com.example.employee_management_system.dto.auth.LoginRequest;
import com.example.employee_management_system.dto.auth.RegisterRequest;

import java.util.List;
import java.util.Optional;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
    List<UserDto> getAllUsers();
    UserDto getUserById(Long id);
    UserDto createUser(RegisterRequest request);
    UserDto assignRolesToUser(Long userId, java.util.Set<Long> roleIds);
    UserDto unassignRolesFromUser(Long userId, java.util.Set<Long> roleIds);
}
