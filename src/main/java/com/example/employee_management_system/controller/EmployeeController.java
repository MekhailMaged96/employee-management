package com.example.employee_management_system.controller;


import com.example.employee_management_system.dto.CreateEmployeeDto;
import com.example.employee_management_system.dto.CreatedEmployeeDto;
import jakarta.validation.Valid;
import com.example.employee_management_system.dto.EmployeeDto;
import com.example.employee_management_system.dto.UpdatedEmployeeDto;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;


    @PostMapping("/create")
    public ResponseEntity<CreatedEmployeeDto> CreateEmployee(@Valid @RequestBody CreateEmployeeDto employee) {
        return ResponseEntity.ok(employeeService.save(employee));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAll());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<UpdatedEmployeeDto> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return ResponseEntity.ok(employeeService.update(id, employee));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Boolean> deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(Boolean.TRUE);
    }

    @PutMapping("/assign/{employeeId}/department/{departmentId}")
    public ResponseEntity<EmployeeDto> assignEmployeeToDepartment(@PathVariable Long employeeId,
                                                               @PathVariable Long departmentId) {
        return ResponseEntity.ok(employeeService.assignEmployeeToDepartment(employeeId, departmentId));
    }

    @PutMapping("/unassign/{employeeId}/department")
    public ResponseEntity<EmployeeDto> unassignEmployeeFromDepartment(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.unassignEmployeeFromDepartment(employeeId));
    }

}
