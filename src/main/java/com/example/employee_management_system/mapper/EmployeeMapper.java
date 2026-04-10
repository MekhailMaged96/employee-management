package com.example.employee_management_system.mapper;

import com.example.employee_management_system.dto.CreateEmployeeDto;
import com.example.employee_management_system.dto.EmployeeDto;
import com.example.employee_management_system.entity.Employee;
import org.mapstruct.Mapper;
import org.springframework.context.annotation.Bean;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    EmployeeDto toDto(Employee employee);
    Employee toEntity(CreateEmployeeDto createEmployeeDto);
}

