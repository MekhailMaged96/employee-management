package com.example.employee_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentFileDto {
    private UUID id;
    private UUID folderId;
    private String originalFileName;
    private String storedFileName;
    private String mimeType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String fileStoragePath;
}

