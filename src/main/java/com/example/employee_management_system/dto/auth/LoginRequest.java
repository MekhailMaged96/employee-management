package com.example.employee_management_system.dto.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}