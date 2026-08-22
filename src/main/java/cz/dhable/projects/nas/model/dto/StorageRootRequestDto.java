package cz.dhable.projects.nas.model.dto;

public class StorageRootRequestDto { // DTO pro přidání disku
    private final String basePath;
    private final String diskName;

    public StorageRootRequestDto(String basePath, String diskName) {
        this.basePath = basePath;
        this.diskName = diskName;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getDiskName() {
        return diskName;
    }
}
