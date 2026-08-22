package cz.dhable.projects.nas.service;

import cz.dhable.projects.nas.model.dto.FileResponseDto;
import cz.dhable.projects.nas.model.dto.FolderContentDto;
import cz.dhable.projects.nas.model.dto.FolderResponseDto;
import cz.dhable.projects.nas.model.dto.FolderTreeDto;
import cz.dhable.projects.nas.model.entity.Folder;
import cz.dhable.projects.nas.model.entity.SharePermission;
import cz.dhable.projects.nas.model.entity.StoredFile;
import cz.dhable.projects.nas.model.entity.User;
import cz.dhable.projects.nas.repository.FolderRepository;
import cz.dhable.projects.nas.repository.SharePermissionRepository;
import cz.dhable.projects.nas.repository.StoredFileRepository;
import cz.dhable.projects.nas.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FolderService {

    private final UserService userService;
    private final StorageService storageService;

    private final FolderRepository folderRepository;
    private final StoredFileRepository fileRepository;
    private final SharePermissionRepository sharePermissionRepository;
    private final UserRepository userRepository;

    public FolderService(FolderRepository folderRepository, StoredFileRepository fileRepository, UserService userService, StorageService storageService, SharePermissionRepository sharePermissionRepository, UserRepository userRepository) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.userService = userService;
        this.storageService = storageService;
        this.sharePermissionRepository = sharePermissionRepository;
        this.userRepository = userRepository;
    }

    /**
     * vytvoření nové složky v systému, pouze v db
     */
    @Transactional
    public Folder createFolder(String name, Long parentId, String userName) {
        User owner = userService.findByUsername(userName); // přihlášený uživ. = vlastník
        if (owner == null) throw new RuntimeException("User was not found");

        Folder parent = null;
        if (parentId != null) {
            parent = folderRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent folder was not found"));
        }

        Folder folder = new Folder(name, parent, owner);
        return folderRepository.save(folder);
    }

    /**
     * vyhození složky do koše (Soft-delete)
     * pokud smažeme složku, do koše jdou i všechny podsložky a soubory uvnitř ní
     */
    @Transactional
    public void moveFolderToTrash(Long folderId, String userName) {
        User user = userService.findByUsername(userName); // přihlášený uživ. = vlastník
        if (user == null) throw new RuntimeException("User was not found");

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("The folder was not found"));

        if (!folder.getOwner().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("You do not have permission to delete this folder");
        }

        LocalDateTime now = LocalDateTime.now();
        cascadeTrash(folder, now);
    }

    // pomocná metoda pro rekurzivní mazání obsahu složky (přesun do koše)
    private void cascadeTrash(Folder folder, LocalDateTime now) {
        folder.setDeleted(true); // označ jako smazané
        folder.setDeletedAt(now); // čas označení je teď
        folderRepository.save(folder); // ulož to do tabulky

        // do koše jdou všechny soubory v této složce; bez ohledu na majitele - vlastník nadřazené složky je akt. uživ.
        List<StoredFile> files = fileRepository.findByFolderAndDeletedFalse(folder);
        for (StoredFile file : files) {
            file.setDeleted(true);
            file.setDeletedAt(now);
            fileRepository.save(file);
        }

        // rekurzivně projdeme a vyhodíme do koše všechny podsložky
        List<Folder> subFolders = folderRepository.findByParentAndDeletedFalse(folder);
        for (Folder subFolder : subFolders) {
            cascadeTrash(subFolder, now); // rekurzivně na podsložky
        }
    }

    @Transactional(readOnly = true) // transakce drží spojení po celou dobu běhu této metody
    public FolderContentDto getFolderContent(Long folderId, String userName) {
        User user = userService.findByUsername(userName); // přihlášený uživ. = vlastník
        if (user == null) throw new RuntimeException("User was not found");

        Folder currentFolder = null;
        if (folderId != null) {
            currentFolder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("The folder was not found"));

            // konrola, zda má uživatel právo jít do této složky
            boolean hasAccess = currentFolder.getOwner().getId().equals(user.getId())
                    || user.getRole().name().equals("ADMIN")
                    || sharePermissionRepository.findBySharedWithAndFolder(user, currentFolder).isPresent();

            if (!hasAccess) {
                throw new SecurityException("You do not have permission to access this folder");
            }
        }


        // načtení podsložek
        List<Folder> subFoldersEntities;
        if (currentFolder == null) {// jsme v rootu NASu

            // najdeme nesmazané složky, které patří tomuto uživateli v této složce
            subFoldersEntities = folderRepository.findByOwnerAndParentAndDeletedFalse(user, null);

            // přidáme složky, které mu nasdíleli ostatní
            List<SharePermission> sharedFolders = sharePermissionRepository.findBySharedWithAndFolderIsNotNull(user);
            for (SharePermission perm : sharedFolders) {
                if (perm.getFolder().getParent() == null && !perm.getFolder().isDeleted()) {
                    // nasdílené složky v rootu NASu; a nejsou smazané
                    subFoldersEntities.add(perm.getFolder());
                }
            }
        } else { // jsme uvnitř nějaké složky
            // zobrazíme podsložky bez ohledu na vlastníka (protože do nadřazené složky už přístup máme)
            subFoldersEntities = folderRepository.findByParentAndDeletedFalse(currentFolder);
        }


        // načtení souborů
        List<StoredFile> filesEntities;
        if (currentFolder == null) { // root NASu
            // najdeme nesmazané soubory, které patří tomuto uživateli v této složce
            filesEntities = fileRepository.findByOwnerAndFolderAndValidDisks(user.getId(), null);

            // přidáme cizí soubory, které my byly nasdíleny přímo do rootu NASu
            List<SharePermission> sharedFiles = sharePermissionRepository.findBySharedWithAndStoredFileIsNotNull(user);
            for (SharePermission perm : sharedFiles) {
                if (perm.getStoredFile().getFolder() == null && !perm.getStoredFile().isDeleted()) { // v rootu NASu; není smazaný
                    filesEntities.add(perm.getStoredFile());
                }
            }
        } else { // nějaká podsložka
            // neřešíme vlastníka bereme vše, protože máme přístup k nadřazenému adresáři
            filesEntities = fileRepository.findByFolderAndDeletedFalse(currentFolder);
        }

        // mapování entit na DTO (přes Java Stream API)
        List<FolderResponseDto> subFolders = subFoldersEntities.stream()
                .map(f -> new FolderResponseDto(f.getId(), f.getName(), f.getParent() != null ? f.getParent().getId() : null, f.getOwner().getUsername(), f.getCreatedAt()))
                .toList();

        List<FileResponseDto> files = filesEntities.stream()
                .map(f -> new FileResponseDto(f.getId(), f.getOriginalName(), f.getFileSize(), f.getContentType(), f.getCreatedAt(), f.getEditedAt(), f.getOwner().getUsername()))
                .toList();

        return new FolderContentDto(subFolders, files);
    }


    /**
     * načte seznam všeho, co má tenhle uživatel v koši
     */
    @Transactional(readOnly = true)
    public FolderContentDto getTrashContent(String currentUsername) {
        User user = userService.findByUsername(currentUsername); // přihlášený uživ. = vlastník
        if (user == null) throw new RuntimeException("User was not found");

        List<Folder> trashedFolders = folderRepository.findByOwnerAndDeletedTrue(user);
        List<StoredFile> trashedFiles = fileRepository.findByOwnerAndDeletedTrue(user);

        // mapování na DTO (stejné jako u normálního obsahu)
        List<FolderResponseDto> subFolders = trashedFolders.stream()
                .map(f -> new FolderResponseDto(f.getId(), f.getName(), f.getParent() != null ? f.getParent().getId() : null, f.getOwner().getUsername(),f.getCreatedAt()))
                .toList();

        List<FileResponseDto> files = trashedFiles.stream()
                .map(f -> new FileResponseDto(f.getId(), f.getOriginalName(), f.getFileSize(), f.getContentType(), f.getCreatedAt(), f.getEditedAt(), f.getOwner().getUsername()))
                .toList();

        return new FolderContentDto(subFolders, files);
    }

    /**
     * obnova složky z koše zpět na původní místo
     */
    @Transactional
    public void restoreFolderFromTrash(Long folderId, String currentUsername) {
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new RuntimeException("The folder was not found"));

        if (!folder.getOwner().getUsername().equals(currentUsername)) {
            throw new SecurityException("You do not have permission to restore this folder");
        }

        // rekurzivně obnovíme složku, její soubory i podsložky
        cascadeRestore(folder);
    }

    private void cascadeRestore(Folder folder) {
        folder.setDeleted(false);
        folder.setDeletedAt(null);
        folderRepository.save(folder);

        // obnovíme soubory uvnitř této složky, které byly smazány společně s ní
        List<StoredFile> files = fileRepository.findByFolderAndDeletedTrue(folder);
        for (StoredFile file : files) {
            file.setDeleted(false);
            file.setDeletedAt(null);
            fileRepository.save(file);
        }

        // rekurzivní obnovení podsložek; od aktuální po 1. podsložky, podsložky podsložek, atd...
        List<Folder> subFolders = folderRepository.findByParentAndDeletedTrue(folder);
        for (Folder subFolder : subFolders) {
            cascadeRestore(subFolder);
        }
    }


    @Transactional
    public void permanentlyDeleteFolder(Long folderId, String currentUsername) throws IOException {
        User user = userService.findByUsername(currentUsername); // přihlášený uživ. = vlastník
        if (user == null) throw new RuntimeException("User was not found");

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder was not found"));

        if (!folder.getOwner().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("You do not have permission to permanently delete this folder.");
        }

        if (!folder.isDeleted()) {
            throw new IllegalStateException("The folder cannot be deleted until it is in the Bin.");
        }

        // rekurzivní odstranění všeho vnitřního obsahu
        cascadePermanentDelete(folder);
    }

    private void cascadePermanentDelete(Folder folder) throws IOException {
        // fyzicky smažeme všechny soubory co jsou v této složce z NAS disku
        List<StoredFile> files = fileRepository.findByFolderAndDeletedTrue(folder);
        for (StoredFile file : files) {
            fileRepository.delete(file); // smazat z DB daný soubor v této složce
            storageService.delete(file.getPhysicalPath()); // smazat z disku NASu, dle fyzické cesty
        }

        // rekruzivní průchod, smazání všech podsložek
        List<Folder> subFolders = folderRepository.findByParentAndDeletedTrue(folder);
        for (Folder subFolder : subFolders) {
            cascadePermanentDelete(subFolder);
        }

        // nakonec smazat z DB aktuální složku
        folderRepository.delete(folder);
    }



    @Transactional
    public void renameFolder(Long folderId, String newName, String currentUsername) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder was not found"));


        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User was not found"));

        boolean hasWriteAccess = folder.getOwner().getId().equals(currentUser.getId())
                || currentUser.getRole().name().equals("ADMIN")
                || sharePermissionRepository.findBySharedWithAndFolder(currentUser, folder)
                .map(SharePermission::isCanWrite).orElse(false);

        if (!hasWriteAccess) {
            throw new SecurityException("You do not have permission to rename this folder");
        }

        folder.setName(newName); // pouze změna v DB
        folderRepository.save(folder);
    }

    @Transactional
    public void moveFolder(Long folderId, Long targetParentId, String currentUsername) {
        Folder folder = folderRepository.findById(folderId).orElseThrow();

        // nelze přesunout složku do ní samotné
        if (folderId.equals(targetParentId)) { // id akt. adresáře je stejné jako id adresáře kam tento přesouváme
            throw new IllegalArgumentException("This folder cannot be moved to the same folder.");
        }

        Folder targetParent = null; // root NASu
        if (targetParentId != null) {
            targetParent = folderRepository.findById(targetParentId)
                    .orElseThrow(() -> new RuntimeException("Destination folder was not found"));

            // cílová složka nesmí být v podstromu pod tou aktuální (neleží kdekoli uvnitř přesouvané složky)
            if (isChildOf(targetParent, folderId)) {
                throw new IllegalArgumentException("Critical error: You cannot move folder into its own subfolder.");
            }
        }

        // opět jen přepis v DB, na disku toto není
        folder.setParent(targetParent);
        folderRepository.save(folder);
    }

    /**
     * pomocná rek. metoda, která jde stromem od cíle nahoru do rootu
     * pokud cestou potká ID přesouvané složky, vrací true (je v podstramu)
     */
    private boolean isChildOf(Folder currentFolder, Long folderIdToFind) {
        if (currentFolder == null) {
            return false; // až do rootu NASu; cyklus neexistuje
        }

        if (currentFolder.getId().equals(folderIdToFind)) { // porovnání id adresářů
            return true; // shoda, cílová složka je potomkem
        }

        // jdeme o úroveň výš k rodiči aktuální složky
        return isChildOf(currentFolder.getParent(), folderIdToFind);
    }


    /**
     * vygeneruje komplet strom nesmazaných složek pro přesun souborů/složek na frontendu
     */
    @Transactional(readOnly = true)
    public List<FolderTreeDto> getFolderTree(String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User was not found."));

        // najdeme všechny složky tohoto uživatele (které přímo vlastní) v rootu NASu (a nejsou v koši)
        List<Folder> rootFolders = folderRepository.findByOwnerAndParentAndDeletedFalse(currentUser, null);

        // tvoříme rekurzivní stromový DTO
        return rootFolders.stream() // pro každý prvek z rootFolders "for(Folder f : rootFolders)"
                .map(this::buildTreeNodes) // zavolá instanční metody buildTreeNodes s argumentem aktuálního adresáře z listu
                .toList(); // celé převede na java List
    }

    // rekurzivní metoda, která prochází podsložky do libovolné hloubky
    private FolderTreeDto buildTreeNodes(Folder folder) {
        // najdeme podsložky této konkrétní složky; opět uživ. stejný (je vlastník); nesmazaná
        List<Folder> subFolders = folderRepository.findByOwnerAndParentAndDeletedFalse(folder.getOwner(), folder);

        List<FolderTreeDto> children = subFolders.stream()
                .map(this::buildTreeNodes) // rek. volání pro hlubší složky
                .toList();

        return new FolderTreeDto(folder.getId(), folder.getName(), children);
    }


}
