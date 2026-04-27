package com.example.employee_management_system.dto;

import com.example.employee_management_system.dto.auth.DepartmentDto;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class UpdatedEmployeeDto {
    private Long id;
    private String name;
    private double salary;
}
