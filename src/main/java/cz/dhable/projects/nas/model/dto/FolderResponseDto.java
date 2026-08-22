package cz.dhable.projects.nas.model.dto;

import java.time.LocalDateTime;

public class FolderResponseDto {
    private final Long id;
    private final String name;
    private final Long parentId;
    private final String ownerUsername;
    private final LocalDateTime createdAt;

    public FolderResponseDto(Long id, String name, Long parentId, String ownerUsername, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
