package cz.dhable.projects.nas.model.dto;

import java.util.UUID;

public class SharePermissionResponseDto {
    private final Long id; // ID samotného záznamu sdílení (pro zrušení sdílení)
    private final UserOutputReqDto sharedWith; // s jakým uživatelem sdílí tento soubor/složku (uživ. jméno; unikátní)
    private final FileResponseDto file;        // soubor nebo null, pokud sdílíme složku
    private final FolderResponseDto folder;    // složka nebo null, pokus sdílíme soubor
    private final boolean canWrite; // true = read i write; false = read

    public SharePermissionResponseDto(Long id, UserOutputReqDto sharedWith, FileResponseDto file, FolderResponseDto folder, boolean canWrite) {
        this.id = id;
        this.sharedWith = sharedWith;
        this.file = file;
        this.folder = folder;
        this.canWrite = canWrite;
    }

    public Long getId() {
        return id;
    }

    public UserOutputReqDto getSharedWith() {
        return sharedWith;
    }

    public FileResponseDto getFile() {
        return file;
    }

    public FolderResponseDto getFolder() {
        return folder;
    }

    public boolean isCanWrite() {
        return canWrite;
    }
}
