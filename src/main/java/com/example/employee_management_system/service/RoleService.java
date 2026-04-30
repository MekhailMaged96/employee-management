package com.example.employee_management_system.service;

import com.example.employee_management_system.dto.RoleDto;

import java.util.List;

public interface RoleService {
    RoleDto create(RoleDto roleDto);
    List<RoleDto> getAll();
    RoleDto getById(Long id);
    RoleDto update(Long id, RoleDto roleDto);
    void delete(Long id);
}

