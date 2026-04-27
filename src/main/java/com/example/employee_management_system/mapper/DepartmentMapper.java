package com.example.employee_management_system.mapper;


import com.example.employee_management_system.dto.auth.DepartmentDto;
import com.example.employee_management_system.entity.Department;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    DepartmentDto toDto(Department department);
    Department toEntity(DepartmentDto departmentDto);
}
