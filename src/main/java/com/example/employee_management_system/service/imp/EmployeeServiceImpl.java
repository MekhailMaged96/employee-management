package com.example.employee_management_system.service.imp;

import com.example.employee_management_system.dto.CreateEmployeeDto;
import com.example.employee_management_system.dto.CreatedEmployeeDto;
import com.example.employee_management_system.dto.EmployeeDto;
import com.example.employee_management_system.dto.UpdatedEmployeeDto;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.exception.ResourceNotFoundException;
import com.example.employee_management_system.mapper.EmployeeMapper;
import com.example.employee_management_system.repository.EmployeeRepository;
import com.example.employee_management_system.repository.DepartmentRepository;
import com.example.employee_management_system.dto.auth.RegisterRequest;
import com.example.employee_management_system.service.AuthService;
import com.example.employee_management_system.repository.UserRepository;
import com.example.employee_management_system.service.EmployeeService;
import com.example.employee_management_system.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository  employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final DepartmentRepository departmentRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    @Override
    public CreatedEmployeeDto  save(CreateEmployeeDto employee) {
        // create user first using AuthService to keep user creation logic consistent
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(employee.getUsername());
        registerRequest.setEmail(employee.getEmail());
        registerRequest.setPassword(employee.getPassword());
        registerRequest.setFullName(employee.getName());

        // create user (may throw BusinessException if username exists)
        authService.createUser(registerRequest);

        // fetch the saved user entity
        User user = userRepository.findByUsername(employee.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after creation: " + employee.getUsername()));

        Employee  employeeEntityc = employeeMapper.toEntity(employee);
        employeeEntityc.setUser(user);

        Employee saved = employeeRepository.save(employeeEntityc);

        return employeeMapper.toCreatedDto(saved);

    }

    @Override
    public EmployeeDto getById(Long id) {
        Employee emp = employeeRepository.findWithDepartmentById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toDto(emp);
    }

    @Override
    public List<EmployeeDto> getAll() {
        return employeeRepository.findAll().stream()
                .map(employeeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UpdatedEmployeeDto update(Long id, Employee employee) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        emp.setName(employee.getName());
        emp.setSalary(employee.getSalary());
        Employee updatedEmp = employeeRepository.save(emp);
        return employeeMapper.toUpdatedDto(updatedEmp);
    }

    @Override
    public void delete(Long id) {
        try {
            employeeRepository.deleteById(id);
        } catch (EmptyResultDataAccessException ex) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
    }

    // internal helper to fetch entity by id, throws if not found
    private Employee getByIdInternal(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    @Override
    public EmployeeDto assignEmployeeToDepartment(Long employeeId, Long departmentId) {
        Employee emp = getByIdInternal(employeeId);
        var dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));
        emp.setDepartment(dept);
        employeeRepository.save(emp);

        return employeeMapper.toDto(emp);
    }

    @Override
    public EmployeeDto unassignEmployeeFromDepartment(Long employeeId) {
        Employee emp = getByIdInternal(employeeId);
        emp.setDepartment(null);
        employeeRepository.save(emp);
        return employeeMapper.toDto(emp);
    }
}
