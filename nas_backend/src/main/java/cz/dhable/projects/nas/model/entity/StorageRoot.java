package cz.dhable.projects.nas.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "storage_roots")
public class StorageRoot { // reálné disky které chceme zapojit do NASu
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) // musí být unikátní!! pro různý disky tak bude stále existovat jedna cesta
    private String basePath; // třeba "/mnt/nas_disk_1"

    @Column(nullable = false)
    private boolean active; // smí se na něj aktuálně nahrávat

    @Column(nullable = false)
    private String diskName; // zobrazené jméno pro ADMINA

    protected StorageRoot() {
        // JPA bezparam. konstr.
    }

    public StorageRoot(String basePath, String diskName) {
        this.basePath = basePath;
        this.diskName = diskName;
    }

    public Long getId() { return id; }
    public String getBasePath() { return basePath; }
    public boolean isActive() { return active; }
    public String getDiskName() { return diskName; }

    public void setActive(boolean active) { this.active = active; }
}
