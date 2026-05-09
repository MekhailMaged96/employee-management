package com.example.employee_management_system.service.imp;

import com.example.employee_management_system.constants.CacheNames;
import com.example.employee_management_system.dto.RoleDto;
import com.example.employee_management_system.entity.Role;
import com.example.employee_management_system.exception.BusinessException;
import com.example.employee_management_system.exception.ResourceNotFoundException;
import com.example.employee_management_system.repository.RoleRepository;
import com.example.employee_management_system.service.RoleService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private  final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

	private final RoleRepository roleRepository;

	@Override
	@Transactional
	@CacheEvict(value = {CacheNames.ROLES, CacheNames.ROLE_BY_ID}, allEntries = true)
	public RoleDto create(RoleDto roleDto) {
		roleRepository.findByName(roleDto.getName()).ifPresent(r -> {
			throw new BusinessException("Role already exists with name: " + roleDto.getName());
		});

		Role role = Role.builder().name(roleDto.getName()).build();
		Role saved = roleRepository.save(role);
		return new RoleDto(saved.getId(), saved.getName());
	}

    @Override
    @Cacheable(value = CacheNames.ROLES)
    public List<RoleDto> getAll() {
        log.info("Fetching all roles from database");
        return roleRepository.findAll().stream()
                .map(r -> new RoleDto(r.getId(), r.getName()))
                .collect(Collectors.toList());
    }

	@Override
	@Cacheable(value = CacheNames.ROLE_BY_ID, key = "#id")
	public RoleDto getById(Long id) {
		Role r = roleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
		return new RoleDto(r.getId(), r.getName());
	}


	@Override
	@Transactional
	@CacheEvict(value = {CacheNames.ROLES, CacheNames.ROLE_BY_ID}, allEntries = true)
	public RoleDto update(Long id, RoleDto roleDto) {
		Role role = roleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

		// if name changed, ensure no other role with same name
		if (!role.getName().equals(roleDto.getName())) {
			roleRepository.findByName(roleDto.getName()).ifPresent(r -> {
				throw new BusinessException("Another role already exists with name: " + roleDto.getName());
			});
		}

		role.setName(roleDto.getName());
		Role saved = roleRepository.save(role);
		return new RoleDto(saved.getId(), saved.getName());
	}

	@Override
	@Transactional
	@CacheEvict(value = {CacheNames.ROLES, CacheNames.ROLE_BY_ID}, allEntries = true)
	public void delete(Long id) {
		Role role = roleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
		roleRepository.delete(role);
	}
}


