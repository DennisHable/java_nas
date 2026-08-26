package cz.dhable.projects.nas.model.dto;

import java.util.UUID;

public class ShareRequestDto {
    private final String shareWithUsername; // s kým sdílíme
    private final UUID fileId;             // ID souboru (null, pokud sdílíme složku)
    private final Long folderId;           // ID složky (null, pokud sdílíme soubor)
    private final boolean canWrite;         // false = pouze čtení; true = čtení a zápis

    public ShareRequestDto(String shareWithUsername, UUID fileId, Long folderId, boolean canWrite) {
        this.shareWithUsername = shareWithUsername;
        this.fileId = fileId;
        this.folderId = folderId;
        this.canWrite = canWrite;
    }

    public String getShareWithUsername() {
        return shareWithUsername;
    }

    public UUID getFileId() {
        return fileId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public boolean isCanWrite() {
        return canWrite;
    }
}