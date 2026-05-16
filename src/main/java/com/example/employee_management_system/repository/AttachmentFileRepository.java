package com.example.employee_management_system.repository;


import com.example.employee_management_system.entity.AttachmentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public  interface AttachmentFileRepository extends JpaRepository<AttachmentFile, UUID> {
	java.util.List<AttachmentFile> findByFolderIdOrderByUploadedAtDesc(java.util.UUID folderId);

}