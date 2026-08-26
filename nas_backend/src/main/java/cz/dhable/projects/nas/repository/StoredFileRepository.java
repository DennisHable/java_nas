package cz.dhable.projects.nas.repository;

import cz.dhable.projects.nas.model.entity.Folder;
import cz.dhable.projects.nas.model.entity.StoredFile;
import cz.dhable.projects.nas.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    List<StoredFile> findByOwnerAndFolderAndDeletedFalse(User owner, Folder folder);
    List<StoredFile> findByFolderAndDeletedFalse(Folder folder);
    List<StoredFile> findByOwnerAndDeletedTrue(User owner);
    List<StoredFile> findByFolderAndDeletedTrue(Folder folder); // najde soubory, kde platí, že adresář je stejný jako v param a deleted==true
    List<StoredFile> findByDeletedTrueAndDeletedAtBefore(LocalDateTime limitDate);


    /**
     * vytáhne soubory ve složce pouze tehdy,
     * pokud jejich fyzická cesta začíná na některou z cest registrovaných v storage_roots.
     * pokud admin disk smaže z DB, soubory disku z průzkumníku okamžitě zmizí
     */
    @Query(value = "SELECT sf.* FROM stored_files sf " + // vybereme soubory
            "WHERE sf.owner_id = :ownerId " + // kde je vlastník akt. uživ.
            "AND (:folderId IS NULL AND sf.folder_id IS NULL OR sf.folder_id = :folderId) " + // v této dané složce; null je root NAS disku
            "AND sf.deleted = false " + // není smazaný
            "AND EXISTS" + // existuje aspoň jeden
            "(SELECT 1 FROM storage_roots sr " + // záznam v tabulce storageroot
            "WHERE sf.physical_path LIKE CONCAT(sr.base_path, '/%'))", // tak že fyzická cesta k tomu souboru obsahuje ten konkrétní basePath (cesta k rootu NAS disku)
            nativeQuery = true) // kontrolu dotazu provede DB
    List<StoredFile> findByOwnerAndFolderAndValidDisks(@Param("ownerId") Long owner, @Param("folderId") Long folder);

}
