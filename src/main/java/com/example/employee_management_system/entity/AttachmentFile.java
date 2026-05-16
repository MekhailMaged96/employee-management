package com.example.employee_management_system.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentFile {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private AttachmentFolder folder;

    @Column(name = "folder_id", insertable = false, updatable = false)
    private UUID folderId;

    private String originalFileName;      // User uploaded filename (e.g., "profile.jpg")
    private String storedFileName;        // Stored on disk (e.g., "att_abc123.jpg")
    private String mimeType;              // Content type (e.g., "image/jpeg")
    private Long fileSize;                // File size in bytes
    private LocalDateTime uploadedAt;     // Upload timestamp
    private String fileStoragePath;       // Relative or absolute path to the file

}

