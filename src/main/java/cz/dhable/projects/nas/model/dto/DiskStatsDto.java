package cz.dhable.projects.nas.model.dto;

public class DiskStatsDto {
    private final String diskName;
    private final String basePath;
    private final boolean active;
    private final long totalBytes;
    private final long usedBytes;
    private final long freeBytes;

    public DiskStatsDto(String diskName, String basePath, boolean active, long totalBytes, long usedBytes, long freeBytes) {
        this.diskName = diskName;
        this.basePath = basePath;
        this.active = active;
        this.totalBytes = totalBytes;
        this.usedBytes = usedBytes;
        this.freeBytes = freeBytes;
    }

    public String getDiskName() {
        return diskName;
    }

    public String getBasePath() {
        return basePath;
    }

    public boolean isActive() {
        return active;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public long getFreeBytes() {
        return freeBytes;
    }
}
