package cz.dhable.projects.nas.model.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stored_files")
public class StoredFile implements Persistable<UUID> {
    @Id
    private UUID id;

    @Column(name="original_name", nullable = false)
    private String originalName; // původní název souboru

    @Column(name="physical_path", nullable = false, unique = true)
    private String physicalPath; // skutečná cesta na disku NASu (ideálně pod UUID názvem, kvůli kolizím)

    @Column(name="file_size", nullable = false)
    private Long fileSize; // velikost v bajtech

    @Column(name="content_type", nullable = false)
    private String contentType; // typ souboru (audio/mpeg, application/pdf, image/jpeg)

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt; // datum,čas vytvoření/nahrání

    @Column(name="edited_at", nullable = false)
    private LocalDateTime editedAt; // datum,čas editace

    @Column(nullable = false)
    private boolean deleted = false; // je soubor smazán; přesunut do koše

    @Column(name="deleted_at")
    private LocalDateTime deletedAt; // datum,čas smazání

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder; // v jaké složce soubor je (null == root)

    // jeden uživatel může vlastnit více souborů
    // LAZY = data o majiteli se z db nenačtou hned, ale až když je opravdu 1. použijeme
    @ManyToOne(fetch = FetchType.LAZY)
    // v db bude sloupec "owner_id" (id uživ.; cizí klíč; určeno na základě typu proměnné (User), propojí id v User s tímto sloupcem);
    // neprázdné = každý soubor má majitele
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // vlastník souboru pro kontrolu práv

    // když se smaže tento soubor, tak se smažou všechny řádky, které na něj někde odkazují
    @OneToMany(mappedBy = "storedFile", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<SharePermission> sharePermissions;


    @Transient // neukládá se do DB, slouží jen pro Hibernate
    private boolean isNew = true;




    protected StoredFile() {
        // bezparam. konstruktor pro JPA
    }

    public StoredFile(UUID id, String originalName, String physicalPath, Long fileSize, String contentType, Folder folder, User owner) {
        this.id = id;
        this.originalName = originalName;
        this.physicalPath = physicalPath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.createdAt = LocalDateTime.now();
        this.editedAt = createdAt;
        this.folder = folder;
        this.owner = owner;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    // touto metodou Hibernate po načtení z DB pozná, že už objekt není nový
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getPhysicalPath() {
        return physicalPath;
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

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Folder getFolder() {
        return folder;
    }

    public User getOwner() {
        return owner;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public void setPhysicalPath(String physicalPath) {
        this.physicalPath = physicalPath;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
