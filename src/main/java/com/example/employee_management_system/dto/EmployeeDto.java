package com.example.employee_management_system.dto;

import com.example.employee_management_system.entity.Employee;
import lombok.Data;

@Data
public class EmployeeDto {

    private  Long id;
    private String name;
    private String email;
    private double salary;

    // helper mapper to convert entity -> dto
    public static EmployeeDto toDto(Employee e) {
        if (e == null) return null;
        EmployeeDto dto = new EmployeeDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setEmail(e.getEmail());
        dto.setSalary(e.getSalary());
        return dto;
    }
}
