package com.example.employee_management_system.mapper;

import com.example.employee_management_system.dto.AttachmentFileDto;
import com.example.employee_management_system.entity.AttachmentFile;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    // map the folderId field directly to dto.folderId to avoid initializing the folder relation
    @org.mapstruct.Mapping(source = "folderId", target = "folderId")
    AttachmentFileDto toDto(AttachmentFile attachment);

    // convert dto to entity; note: folder reference should be set by service if needed
    AttachmentFile toEntity(AttachmentFileDto attachmentFileDto);

    // keep helper conversions if MapStruct needs them (not required here but harmless)
    default UUID map(String value) {
        return value != null ? UUID.fromString(value) : null;
    }

    default String map(UUID value) {
        return value != null ? value.toString() : null;
    }

}
