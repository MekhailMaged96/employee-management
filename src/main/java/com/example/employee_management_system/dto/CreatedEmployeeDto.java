package com.example.employee_management_system.dto;

import lombok.Data;

@Data
public class CreatedEmployeeDto {
    private Long id;
    private String name;
    private double salary;
    private UserDto user;
}
