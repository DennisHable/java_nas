package cz.dhable.projects.nas.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class FileResponseDto {
    private final UUID id;
    private final String originalName;
    private final Long fileSize;
    private final String contentType;
    private final LocalDateTime createdAt;
    private final LocalDateTime editedAt;
    private final String ownerUsername;

    public FileResponseDto(UUID id, String originalName, Long fileSize, String contentType, LocalDateTime createdAt, LocalDateTime lastEdit, String ownerUsername) {
        this.id = id;
        this.originalName = originalName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.createdAt = createdAt;
        this.editedAt = lastEdit;
        this.ownerUsername = ownerUsername;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }
}
