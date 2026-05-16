package com.example.employee_management_system.service;

import com.example.employee_management_system.dto.AttachmentFileDto;
import com.example.employee_management_system.entity.AttachmentFile;
import com.example.employee_management_system.entity.AttachmentFolder;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.repository.AttachmentFileRepository;
import com.example.employee_management_system.repository.AttachmentFolderRepository;
import com.example.employee_management_system.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Service
@RequiredArgsConstructor
public class AttachmentStorageService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentStorageService.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-file-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${app.upload.allowed-mime-types:image/jpeg,image/png,image/gif,application/pdf}")
    private String allowedMimeTypesProperty;

    private final EmployeeRepository employeeRepository;
    private final AttachmentFolderRepository folderRepository;
    private final AttachmentFileRepository fileRepository;
    private final ApplicationContext applicationContext;
    private final com.example.employee_management_system.mapper.AttachmentMapper attachmentMapper;


    /**
     * Store the uploaded file on disk and persist AttachmentFile & AttachmentFolder metadata.
     */
    @Transactional
    public AttachmentFile store(MultipartFile file, Long employeeId) throws IOException {

        validateFile(file);

        var employee = fetchEmployee(employeeId);
        var folder = findOrCreateFolder(employee, employeeId);

        String original = file.getOriginalFilename();
        String ext = getFileExtension(original);

        if (!isMimeAllowed(file.getContentType())) {
            throw new IOException("File type not allowed: " + file.getContentType());
        }

        String storedFileName = generateStoredFileName(ext);

        Path target = null;

        try {

            target = writeFileToDisk(file, storedFileName);

            var attachmentFile = buildAttachmentFile(
                    folder,
                    original,
                    storedFileName,
                    file,
                    target
            );

            attachmentFile.setFolderId(folder.getId());

            return fileRepository.save(attachmentFile);

        } catch (Exception ex) {

            // cleanup orphan file if DB save fails
            if (target != null) {
                Files.deleteIfExists(target);
            }

            throw ex;
        }
    }

    public AttachmentFile saveAttachmentFile(AttachmentFile file) {
        return fileRepository.save(file);
    }



    // --- Helper methods to make store() small and testable ---

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new IOException("File size exceeds limit: " + maxFileSize);
        }
    }

    private Employee fetchEmployee(Long employeeId) throws IOException {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IOException("Employee not found: " + employeeId));
    }

    private AttachmentFolder findOrCreateFolder(Employee employee, Long employeeId) {
        return folderRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> {
                    AttachmentFolder f = AttachmentFolder.builder()
                            .employee(employee)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return folderRepository.save(f);
                });
    }

    private String getFileExtension(String original) {
        if (original == null) return "";
        int idx = original.lastIndexOf('.');
        if (idx == -1 || idx == original.length() - 1) return "";
        return original.substring(idx + 1);
    }

    private boolean isMimeAllowed(String mime) {
        if (mime == null) return false;
        String[] allowed = allowedMimeTypesProperty.split(",");
        for (String a : allowed) {
            a = a.trim();
            if (a.equalsIgnoreCase(mime)) return true;
            if (a.endsWith("/*")) {
                String prefix = a.substring(0, a.indexOf('/'));
                if (mime.startsWith(prefix + "/")) return true;
            }
        }
        return false;
    }

    private String generateStoredFileName(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return extension == null || extension.isEmpty() ? "att_" + uuid : "att_" + uuid + "." + extension;
    }

    private Path writeFileToDisk(MultipartFile file, String storedFileName) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        Path target = uploadPath.resolve(storedFileName);
        Files.write(target, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }

    private AttachmentFile buildAttachmentFile(AttachmentFolder folder, String original, String storedFileName, MultipartFile file, Path target) {
        return AttachmentFile.builder()
                .folder(folder)
                .originalFileName(original)
                .storedFileName(storedFileName)
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .fileStoragePath(target.toString())
                .build();
    }

    public Resource loadResourceByFileId(UUID fileId) throws MalformedURLException {
        Optional<AttachmentFile> opt = fileRepository.findById(fileId);
        if (opt.isEmpty()) return null;
        Path p = Paths.get(opt.get().getFileStoragePath());
        UrlResource res = new UrlResource(p.toUri());
        return res.exists() && res.isReadable() ? res : null;
    }

    public Optional<AttachmentFile> getLatestFileForEmployee(Long employeeId) {
        Optional<AttachmentFolder> folderOpt = folderRepository.findByEmployeeId(employeeId);
        if (folderOpt.isEmpty()) return Optional.empty();
        AttachmentFolder folder = folderOpt.get();
        var list = fileRepository.findByFolderIdOrderByUploadedAtDesc(folder.getId());
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(0));
    }

    public Optional<AttachmentFile> getAttachmentFile(UUID fileId) {
        return fileRepository.findById(fileId);
    }

}


