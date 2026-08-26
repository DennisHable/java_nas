package cz.dhable.projects.nas.model.dto;

import java.util.List;

public class FolderContentDto {
    private final List<FolderResponseDto> subFolders;
    private final List<FileResponseDto> files;

    public FolderContentDto(List<FolderResponseDto> subFolders, List<FileResponseDto> files) {
        this.subFolders = subFolders;
        this.files = files;
    }

    public List<FolderResponseDto> getSubFolders() {
        return subFolders;
    }

    public List<FileResponseDto> getFiles() {
        return files;
    }
}
