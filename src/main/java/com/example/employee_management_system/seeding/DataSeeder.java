package com.example.employee_management_system.seeding;

import com.example.employee_management_system.entity.Department;
import com.example.employee_management_system.entity.Role;
import com.example.employee_management_system.repository.DepartmentRepository;
import com.example.employee_management_system.repository.RoleRepository;
import com.example.employee_management_system.constants.RoleConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;

    public DataSeeder(DepartmentRepository departmentRepository,RoleRepository roleRepository) {
        this.roleRepository=roleRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedDepartments();
        seedRoles();
    }


    public void seedDepartments() {
        if (departmentRepository.count() == 0) {
            var departmentNames= List.of("HR", "IT", "Finance", "Marketing", "Sales");
            departmentNames.forEach(name -> {
                Department department = new Department();
                department.setName(name);
                departmentRepository.save(department);
            });
        }
    }


    public void seedRoles() {
        if(this.roleRepository.count()==0){
            var roleNames= List.of(RoleConstants.EMPLOYEE, RoleConstants.ADMIN);
            roleNames.forEach(name->{
                var role=new Role();
                role.setName(name);
                roleRepository.save(role);
            });
        }
    }



}
