package cz.dhable.projects.nas.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "folders")
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // název složky

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name="deleted_at")
    private LocalDateTime deletedAt;

    @Column(name="created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent; // nadřazená složka; null znamená, že je v "rootu" disku

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // vlastník složky

    @OneToMany(mappedBy = "folder", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<SharePermission> sharePermissions;

    protected Folder() {
        // bezparam. konstr. pro JPA
    }

    public Folder(String name, Folder parent, User owner) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.parent = parent;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Folder getParent() {
        return parent;
    }

    public User getOwner() {
        return owner;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setParent(Folder parent) {
        this.parent = parent;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
