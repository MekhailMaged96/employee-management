package com.example.employee_management_system.service;
import com.example.employee_management_system.dto.CreateEmployeeDto;
import com.example.employee_management_system.dto.EmployeeDto;
import com.example.employee_management_system.entity.Employee;
import java.util.List;

public interface EmployeeService {

    Employee save(CreateEmployeeDto employee);
    EmployeeDto getById(Long id);
    List<EmployeeDto> getAll();
    Employee update(Long id, Employee employee);
    void delete(Long id);
}
