package com.example.employee_management_system.repository;
import com.example.employee_management_system.entity.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee,Long> {

    @EntityGraph(attributePaths = {"department"})
    Optional<Employee> findWithDepartmentById(Long id);

    @EntityGraph(attributePaths = {"department","user","user.roles"})
    List<Employee> findAll();
}
