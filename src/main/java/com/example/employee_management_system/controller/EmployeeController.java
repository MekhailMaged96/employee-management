package com.example.employee_management_system.controller;


import com.example.employee_management_system.dto.CreateEmployeeDto;
import com.example.employee_management_system.dto.CreatedEmployeeDto;
import com.example.employee_management_system.mapper.AttachmentMapper;
import com.example.employee_management_system.service.AttachmentStorageService;
import jakarta.validation.Valid;
import com.example.employee_management_system.dto.EmployeeDto;
import com.example.employee_management_system.dto.UpdatedEmployeeDto;
import com.example.employee_management_system.entity.Employee;
import com.example.employee_management_system.service.EmployeeService;
import org.springframework.core.io.Resource;
import com.example.employee_management_system.dto.AttachmentFileDto;
import com.example.employee_management_system.entity.AttachmentFile;
import java.util.UUID;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AttachmentStorageService attachmentStorageService;
    private  final AttachmentMapper attachmentMapper;
    // ── Admin only: create, update, delete, assign ───────────────────
    // hasAuthority matches the exact role name stored in the DB ("Admin")

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<CreatedEmployeeDto> CreateEmployee(@Valid @RequestBody CreateEmployeeDto employee) {
        return ResponseEntity.ok(employeeService.save(employee));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<UpdatedEmployeeDto> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return ResponseEntity.ok(employeeService.update(id, employee));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Boolean> deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.ok(Boolean.TRUE);
    }

    @PutMapping("/assign/{employeeId}/department/{departmentId}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<EmployeeDto> assignEmployeeToDepartment(@PathVariable Long employeeId,
                                                               @PathVariable Long departmentId) {
        return ResponseEntity.ok(employeeService.assignEmployeeToDepartment(employeeId, departmentId));
    }

    @PutMapping("/unassign/{employeeId}/department")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<EmployeeDto> unassignEmployeeFromDepartment(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.unassignEmployeeFromDepartment(employeeId));
    }

    // ── Any authenticated user: read ─────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Employee')")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getById(id));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('Admin', 'Employee')")
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAll());
    }

    // ── Attachment endpoints (moved from EmployeePhotoController / AttachmentController) ──

    @PostMapping(path = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('Admin', 'Employee')")
    public ResponseEntity<?> uploadAttachment(@PathVariable("id") Long id,
                                              @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        try {
            AttachmentFile saved = attachmentStorageService.store(file, id);
            return ResponseEntity.ok(attachmentMapper.toDto(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(path = "/{id}/attachment")
    @PreAuthorize("hasAnyAuthority('Admin', 'Employee')")
    public ResponseEntity<?> downloadAttachment(@PathVariable("id") Long id,
                                                @RequestParam(name = "fileId") UUID fileId) {
        try {
            // fileId is required
            var opt = attachmentStorageService.getAttachmentFile(fileId);
            if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "File not found"));

            var af = opt.get();
            Resource res = attachmentStorageService.loadResourceByFileId(fileId);
            if (res == null) return ResponseEntity.status(404).body(Map.of("error", "Resource not readable"));

            // use mapper to build DTO and extract metadata
            var dto = attachmentMapper.toDto(af);
            String filename = dto.getOriginalFileName();
            String contentType = dto.getMimeType() != null ? dto.getMimeType() : "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(res);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
