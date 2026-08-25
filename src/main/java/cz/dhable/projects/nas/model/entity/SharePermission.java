package cz.dhable.projects.nas.model.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_permissions")
public class SharePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWith; // kome se právo uděluje

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id")
    private StoredFile storedFile; // sdílíme konkrétní soubor (může být null)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder; // sdílíme celou složku (může být null)

    @Column(name="can_write", nullable = false)
    private boolean canWrite = false; // false = pouze čtení (READ), true = čtení i zápis/edit (WRITE)

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected SharePermission() {
        // pro Hibernate
    }

    public SharePermission(User sharedWith, StoredFile storedFile, Folder folder, boolean canWrite) {
        this.sharedWith = sharedWith;
        this.storedFile = storedFile;
        this.folder = folder;
        this.canWrite = canWrite;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getSharedWith() {
        return sharedWith;
    }

    public StoredFile getStoredFile() {
        return storedFile;
    }

    public Folder getFolder() {
        return folder;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }

    public void setSharedWith(User sharedWith) {
        this.sharedWith = sharedWith;
    }

    public void setStoredFile(StoredFile storedFile) {
        this.storedFile = storedFile;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }
}
