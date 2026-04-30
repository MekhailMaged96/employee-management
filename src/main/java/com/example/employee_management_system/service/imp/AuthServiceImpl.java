package com.example.employee_management_system.service.imp;

import com.example.employee_management_system.dto.UserDto;
import com.example.employee_management_system.dto.auth.AuthResponse;
import com.example.employee_management_system.dto.auth.LoginRequest;
import com.example.employee_management_system.dto.auth.RegisterRequest;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.entity.User;
import com.example.employee_management_system.exception.BusinessException;
import com.example.employee_management_system.repository.EmployeeRepository;
import com.example.employee_management_system.repository.RoleRepository;
import com.example.employee_management_system.entity.Role;
import com.example.employee_management_system.constants.RoleConstants;
import java.util.HashSet;
import java.util.Set;
import com.example.employee_management_system.repository.UserRepository;
import com.example.employee_management_system.mapper.UserMapper;
import com.example.employee_management_system.security.jwt.JwtUtil;
import com.example.employee_management_system.service.AuthService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.employee_management_system.exception.ResourceNotFoundException;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }


        User user = userMapper.toEntity(request);
        // encode password before assigning role and saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // assign EMPLOYEE role (create if missing)
        Role employeeRole = roleRepository.findByName(RoleConstants.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleConstants.EMPLOYEE).build()));
        Set<Role> roles = new HashSet<>();
        roles.add(employeeRole);
        user.setRoles(roles);

        User saved = userRepository.save(user);

        Employee employee = new Employee();
        employee.setName(saved.getUsername());
        employee.setUser(saved);


        employeeRepository.save(employee);

        // ✅ collect role names before passing to token
        Set<String> roleNames = saved.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // ✅ embed username, userId AND roles in the token
        String token = jwtUtil.generateToken(saved.getUsername(), saved.getId(), roleNames);

        return new AuthResponse(token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // ✅ use findWithRolesByUsername so roles are eagerly loaded (avoids LazyInitializationException)
        User user = userRepository.findWithRolesByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // ✅ collect role names
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // ✅ embed username, userId AND roles
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), roleNames);

        return new AuthResponse(token);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public  UserDto  getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto createUser(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }
        User user = userMapper.toEntity(request);
        // encode password before assigning role and saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // assign EMPLOYEE role by default
        Role employeeRole = roleRepository.findByName(RoleConstants.EMPLOYEE)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleConstants.EMPLOYEE).build()));
        Set<Role> roles = new HashSet<>();
        roles.add(employeeRole);
        user.setRoles(roles);

        User saved = userRepository.save(user);

        return userMapper.toDto(saved);
    }




    @Override
    @Transactional
    public UserDto assignRolesToUser(Long userId, Set<Long> roleIds) {
        var user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));

        if(roles.size() != roleIds.size()) {

            Set<Long> foundsIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
            Set<Long> missing = new HashSet<>(roleIds);
            missing.removeAll(foundsIds);
            throw new ResourceNotFoundException("Roles not found: " + missing);
        }
        Set<Long> alreadyAssigned = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getId)
                 .filter(roleIds::contains)
                 .collect(Collectors.toSet())
                : new HashSet<>();

        if (!alreadyAssigned.isEmpty()) {
            throw new BusinessException("Roles already assigned to user: " + alreadyAssigned);
        }

        user.getRoles().addAll(roles);

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto unassignRolesFromUser(Long userId, Set<Long> roleIds) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        var currentRoleIds  = user.getRoles().stream().map(Role::getId).collect(Collectors.toSet());

        Set<Long> notAssigned = roleIds.stream()
                .filter(id -> !currentRoleIds.contains(id))
                .collect(Collectors.toSet());

        if(!notAssigned.isEmpty()) {
            throw new BusinessException("Roles not assigned to user: " + notAssigned);
        }

        user.getRoles().removeIf(role -> roleIds.contains(role.getId()));

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }
}
