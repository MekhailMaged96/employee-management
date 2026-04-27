package com.example.employee_management_system.service.imp;

import com.example.employee_management_system.dto.auth.DepartmentDto;
import com.example.employee_management_system.entity.Department;
import com.example.employee_management_system.exception.ResourceNotFoundException;
import com.example.employee_management_system.mapper.DepartmentMapper;
import com.example.employee_management_system.repository.DepartmentRepository;
import com.example.employee_management_system.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    @Override
    public List<DepartmentDto> getAllDepartments() {
        return  departmentRepository.findAll(Sort.by("id").ascending()).stream()
                .map(departmentMapper::toDto)
                .toList();
    }

    @Override
    public DepartmentDto getDepartmentById(Long id) {
        var department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return departmentMapper.toDto(department);

    }

    @Override
    public DepartmentDto createDepartment(DepartmentDto departmentDto) {
        Department department = departmentMapper.toEntity(departmentDto);
        department = departmentRepository.save(department);
        return departmentMapper.toDto(department);
    }

    @Override
    public DepartmentDto updateDepartment(Long id, DepartmentDto departmentDto) {
        var department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        department.setName(departmentDto.getName());
        departmentRepository.save(department);

        return departmentMapper.toDto(department);

    }

    @Override
    public void deleteDepartment(Long id) {
        var department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
         departmentRepository.delete(department);
    }
}
