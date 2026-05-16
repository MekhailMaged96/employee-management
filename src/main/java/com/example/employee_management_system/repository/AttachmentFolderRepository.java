package com.example.employee_management_system.repository;


import com.example.employee_management_system.entity.AttachmentFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public  interface AttachmentFolderRepository extends JpaRepository<AttachmentFolder, UUID> {
	java.util.Optional<AttachmentFolder> findByEmployeeId(Long employeeId);
}