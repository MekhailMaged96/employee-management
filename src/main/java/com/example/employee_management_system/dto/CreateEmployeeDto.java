package com.example.employee_management_system.dto;


import lombok.Data;

@Data
public class CreateEmployeeDto {
    private String name;
    private String email;
    private double salary;
}
