package com.example.employee_management_system.controller;

import com.example.employee_management_system.dto.auth.DepartmentDto;
import com.example.employee_management_system.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public List<DepartmentDto> getAll() {
        return departmentService.getAllDepartments();
    }

    @GetMapping("/{id}")
    public DepartmentDto getById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id);
    }

    @PostMapping
    public DepartmentDto create(@RequestBody DepartmentDto request) {
        return departmentService.createDepartment(request);
    }

    @PutMapping("/{id}")
    public DepartmentDto update(@PathVariable Long id,
                                @RequestBody DepartmentDto request) {
        return departmentService.updateDepartment(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
    }
}
