package com.example.employee_management_system.controller;

import com.example.employee_management_system.dto.auth.DepartmentDto;
import com.example.employee_management_system.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Admin', 'Employee')")
    public List<DepartmentDto> getAll() {
        return departmentService.getAllDepartments();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Employee')")
    public DepartmentDto getById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Admin')")
    public DepartmentDto create(@RequestBody DepartmentDto request) {
        return departmentService.createDepartment(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public DepartmentDto update(@PathVariable Long id,
                                @RequestBody DepartmentDto request) {
        return departmentService.updateDepartment(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public void delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }
}
