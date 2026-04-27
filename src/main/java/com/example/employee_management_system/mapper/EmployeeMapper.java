package com.example.employee_management_system.mapper;

import com.example.employee_management_system.dto.CreateEmployeeDto;
import com.example.employee_management_system.dto.CreatedEmployeeDto;
import com.example.employee_management_system.dto.EmployeeDto;
import com.example.employee_management_system.dto.UpdatedEmployeeDto;
import com.example.employee_management_system.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface EmployeeMapper {

    @Mapping(source = "user", target = "user")
    EmployeeDto toDto(Employee employee);

    Employee toEntity(CreateEmployeeDto createEmployeeDto);

    UpdatedEmployeeDto toUpdatedDto(Employee employee);

    CreatedEmployeeDto toCreatedDto(Employee employee);
}

