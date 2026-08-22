package cz.dhable.projects.nas.repository;

import cz.dhable.projects.nas.model.entity.Folder;
import cz.dhable.projects.nas.model.entity.SharePermission;
import cz.dhable.projects.nas.model.entity.StoredFile;
import cz.dhable.projects.nas.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SharePermissionRepository extends JpaRepository<SharePermission, Long> {

    // najde přímé sdílení pro konkrétní soubor a daného uživetele (pokud takové sdílení existuje)
    Optional<SharePermission> findBySharedWithAndStoredFile(User sharedWith, StoredFile storedFile);

    // najde přímé sdílení pro konkrétní složku
    Optional<SharePermission> findBySharedWithAndFolder(User sharedWith, Folder folder);

    // seznam všech složek, které někdo nasdílel tomuto uživateli
    List<SharePermission> findBySharedWithAndFolderIsNotNull(User sharedWith);

    // najde soubory které jsou sdílé s daným uživatelem a soubor není null
    List<SharePermission> findBySharedWithAndStoredFileIsNotNull(User sharedWith);

    /**
     * oprávnění která udělil aktuální uživatel ostatním
     */
    @Query("SELECT p FROM SharePermission p " + // vybíráme oprávnění
            "LEFT JOIN p.storedFile f " + // spojíme se soubory, abysme mohli kouknout na vlastníka
            "LEFT JOIN p.folder fold " + // ...
            "WHERE f.owner = :owner OR fold.owner = :owner") // je vlastník
    List<SharePermission> findByResourceOwner(@Param("owner") User owner);

    List<SharePermission> findBySharedWith(User sharedWith);
}
