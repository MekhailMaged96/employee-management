package com.example.employee_management_system.service.imp;

import com.example.employee_management_system.dto.CreateEmployeeDto;
import com.example.employee_management_system.dto.EmployeeDto;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.repository.EmployeeRepository;
import com.example.employee_management_system.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository  employeeRepository;

    @Override
    public Employee save(CreateEmployeeDto employee) {

        Employee emp = Employee.builder()
                    .name(employee.getName())
                  .email(employee.getEmail())
                    .salary(employee.getSalary())
                    .build();

        return employeeRepository.save(emp);

    }

    @Override
    public EmployeeDto getById(Long id) {
        Employee emp = employeeRepository.findById(id).orElse(null);
        return EmployeeDto.toDto(emp);
    }

    @Override
    public List<EmployeeDto> getAll() {
        return employeeRepository.findAll().stream()
                .map(EmployeeDto::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Employee update(Long id, Employee employee) {
        Employee emp = getByIdInternal(id);
        emp.setName(employee.getName());
        emp.setEmail(employee.getEmail());
        emp.setSalary(employee.getSalary());
        return employeeRepository.save(emp);
    }

    @Override
    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    // internal helper to fetch entity by id
    private Employee getByIdInternal(Long id) {
        return employeeRepository.findById(id).orElse(null);
    }
}
