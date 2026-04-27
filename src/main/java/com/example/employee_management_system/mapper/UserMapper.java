package com.example.employee_management_system.mapper;

import com.example.employee_management_system.dto.UserDto;
import com.example.employee_management_system.dto.auth.RegisterRequest;
import com.example.employee_management_system.entity.Role;
import com.example.employee_management_system.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "roles", target = "roles", qualifiedByName = "rolesToNames")
    UserDto toDto(User user);

    @Mapping(target = "password", source = "password")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    User toEntity(RegisterRequest request);

    // Explicit method for Set<Role> -> Set<String> conversion
    @Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}

