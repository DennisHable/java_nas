package cz.dhable.projects.nas.repository;

import cz.dhable.projects.nas.model.entity.Folder;
import cz.dhable.projects.nas.model.entity.StoredFile;
import cz.dhable.projects.nas.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByOwnerAndParentAndDeletedFalse(User owner, Folder parent);
    List<Folder> findByParentAndDeletedFalse(Folder parent);
    List<Folder> findByOwnerAndDeletedTrue(User owner);
    List<Folder> findByParentAndDeletedTrue(Folder folder); // všechny složky co mají za nadřazený adresář ten ve folder a delete==true
    List<Folder> findByDeletedTrueAndDeletedAtBefore(LocalDateTime limitDate);
}
