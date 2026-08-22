package cz.dhable.projects.nas.model.dto;

import java.util.List;

public class SystemStatsDto { // DTO pro přenos statistik zatížení OS na frontend
    private final double cpuUsagePercent;
    private final long ramTotalBytes;
    private final long ramUsedBytes;
    private final long ramFreeBytes;
    /*private final long diskTotalBytes;
    private final long diskUsedBytes;
    private final long diskFreeBytes;
*/
    private final List<DiskStatsDto> disks;

    public SystemStatsDto(double cpuUsagePercent, long ramTotalBytes, long ramUsedBytes, long ramFreeBytes, List<DiskStatsDto> disks) {
        this.cpuUsagePercent = cpuUsagePercent;
        this.ramTotalBytes = ramTotalBytes;
        this.ramUsedBytes = ramUsedBytes;
        this.ramFreeBytes = ramFreeBytes;
        this.disks = disks;
    }

    public double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public long getRamTotalBytes() {
        return ramTotalBytes;
    }

    public long getRamUsedBytes() {
        return ramUsedBytes;
    }

    public long getRamFreeBytes() {
        return ramFreeBytes;
    }

    public List<DiskStatsDto> getDisks() {
        return disks;
    }
}
