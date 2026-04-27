package com.example.employee_management_system.service;
import com.example.employee_management_system.dto.CreateEmployeeDto;
import com.example.employee_management_system.dto.CreatedEmployeeDto;
import com.example.employee_management_system.dto.EmployeeDto;
import com.example.employee_management_system.dto.UpdatedEmployeeDto;
import com.example.employee_management_system.entity.Employee;
import java.util.List;

public interface EmployeeService {

    CreatedEmployeeDto save(CreateEmployeeDto employee);
    EmployeeDto getById(Long id);
    List<EmployeeDto> getAll();
    UpdatedEmployeeDto update(Long id, Employee employee);
    void delete(Long id);
    // assign employee to a department
    EmployeeDto assignEmployeeToDepartment(Long employeeId, Long departmentId);
    // remove employee's department association
    EmployeeDto unassignEmployeeFromDepartment(Long employeeId);
}
