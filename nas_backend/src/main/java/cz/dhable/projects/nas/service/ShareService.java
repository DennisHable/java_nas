package cz.dhable.projects.nas.service;

import cz.dhable.projects.nas.model.dto.*;
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

import java.util.List;

@Service
public class ShareService {

    private final SharePermissionRepository sharePermissionRepository;
    private final UserRepository userRepository;
    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;

    public ShareService(SharePermissionRepository sharePermissionRepository, UserRepository userRepository,
                        StoredFileRepository fileRepository, FolderRepository folderRepository) {
        this.sharePermissionRepository = sharePermissionRepository;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
    }

    /**
     * vytvoří nové sdílení pro soubor nebo složku
     */
    @Transactional
    public void shareResource(ShareRequestDto dto, String currentUsername) {
        User owner = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Logged-in user was not found"));

        User targetUser = userRepository.findByUsername(dto.getShareWithUsername())
                .orElseThrow(() -> new RuntimeException("The user with whom you want to share the file/folder does not exist"));

        if (owner.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("You cannot share the file/folder with yourself");
        }

        StoredFile file;
        Folder folder;

        // sdílíme konkrétní soubor
        if (dto.getFileId() != null) {
            file = fileRepository.findById(dto.getFileId())
                        .orElseThrow(() -> new RuntimeException("File was not found"));

            // pouze vlastník může soubor sdílet
            if (!file.getOwner().getId().equals(owner.getId())) {
                throw new SecurityException("You do not have permission to share this file; you are not its owner");
            }

            // pokud už sdílení existuje, pouze aktualizujeme práva canWrite
            sharePermissionRepository.findBySharedWithAndStoredFile(targetUser, file) // vrací optional; buď nějaká práva má (existuje ten záznam), nebo mu ho sdílíme popvé
                    .ifPresentOrElse(
                            perm -> perm.setCanWrite(dto.isCanWrite()), // spustí se jen když záznam v DB existuje; vezme záznam (perm) a upraví prává; Hibernate automaticky uloží na konci metody do DB (změnil se původní stav)
                            () -> sharePermissionRepository.save(new SharePermission(targetUser, file, null, dto.isCanWrite())) // pouze když je Optional prázdný; nemáme object => prázdné závorky; vytvotření a uložení
                    );
        } else if (dto.getFolderId() != null) { // sdílíme celou složku
            folder = folderRepository.findById(dto.getFolderId())
                        .orElseThrow(() -> new RuntimeException("Folder was not found"));

            if (!folder.getOwner().getId().equals(owner.getId())) {
                throw new SecurityException("You do not have permission to share this folder; you are not its owner");
            }

            sharePermissionRepository.findBySharedWithAndFolder(targetUser, folder)
                    .ifPresentOrElse(
                            perm -> perm.setCanWrite(dto.isCanWrite()),
                            () -> sharePermissionRepository.save(new SharePermission(targetUser, null, folder, dto.isCanWrite()))
                    );
        } else {
            throw new IllegalArgumentException("You must specify either fileId or folderId");
        }
    }


    /**
     * vrací seznam všech práv, které aktuální uživatel udělil ostatním uživatelům
     */
    @Transactional(readOnly = true)
    public List<SharePermissionResponseDto> getThingsIShare(String currentUsername) {
        User owner = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User was not found"));

        // najdeme sdílení, kde je přihlášený uživatel vlastníkem souboru nebo složky
        List<SharePermission> permissions = sharePermissionRepository.findByResourceOwner(owner);

        return permissions.stream() // postupně pro každý objekt v list
                .map(p -> new SharePermissionResponseDto( // mapujeme na DTO
                        p.getId(),
                        // uživatel, kterému to sdílíme
                        new UserOutputReqDto(p.getSharedWith().getId(),
                                p.getSharedWith().getUsername(),
                                p.getSharedWith().getRole().name(),
                                p.getSharedWith().getEmail()),
                        // soubor (pokud existuje)
                        p.getStoredFile() != null ? new FileResponseDto(p.getStoredFile().getId(), p.getStoredFile().getOriginalName(), p.getStoredFile().getFileSize(), p.getStoredFile().getContentType(), p.getStoredFile().getCreatedAt(), p.getStoredFile().getEditedAt(), p.getStoredFile().getOwner().getUsername()) : null,
                        // složka (pokud existuje)
                        p.getFolder() != null ? new FolderResponseDto(p.getFolder().getId(), p.getFolder().getName(), p.getFolder().getParent() != null ? p.getFolder().getParent().getId() : null, p.getFolder().getOwner().getUsername(), p.getFolder().getCreatedAt()) : null,
                        p.isCanWrite()
                )).toList();// DTO na jeden list
    }

    /**
     * odebere opravnění k souboru/složcce
     */
    @Transactional
    public void revokePermission(Long permissionId, String currentUsername) {
        SharePermission perm = sharePermissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Sharing was not found"));

        // kontrola; zrušit sdílení může jen vlastník
        User owner = userRepository.findByUsername(currentUsername).orElseThrow();
        boolean isOwner = (perm.getStoredFile() != null && perm.getStoredFile().getOwner().getId().equals(owner.getId())) // sdílí se soubor
                || (perm.getFolder() != null && perm.getFolder().getOwner().getId().equals(owner.getId())); // sdílí se složka

        if (!isOwner) {
            throw new SecurityException("You do not have permission to cancel this sharing.");
        }

        sharePermissionRepository.delete(perm); // smazání záznamu z mezitabulky v DB
    }

    /**
     * vrací seznam všech souborů a složek, které někdo nasdílel tomuto uživ.
     * tedy bez ohledu na adresářovou strukturu, bere to z mezitabulky .
     */
    @Transactional(readOnly = true)
    public FolderContentDto getThingsSharedWithMe(String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername).orElseThrow();

        // řádky z tabulky sdílení; soubory/složky, které se mnou někdo sdílí
        List<SharePermission> permissions = sharePermissionRepository.findBySharedWith(currentUser);

        // mapujeme složky
        List<FolderResponseDto> subFolders = permissions.stream()// pro každý prvek v tom seznamu
                .filter(p -> p.getFolder() != null && !p.getFolder().isDeleted()) // bereme ty záznamy, kde folder není null => je to sdílení souboru a není smazaný
                .map(p -> new FolderResponseDto(p.getFolder().getId(), p.getFolder().getName(), p.getFolder().getParent() != null ? p.getFolder().getParent().getId() : null, p.getFolder().getOwner().getUsername(), p.getFolder().getCreatedAt())) // mapování na DTO pro frontend
                .toList(); // převod na java list

        // stejně mapujeme soubory
        List<FileResponseDto> files = permissions.stream()
                .filter(p -> p.getStoredFile() != null && !p.getStoredFile().isDeleted())
                .map(p -> new FileResponseDto(p.getStoredFile().getId(), p.getStoredFile().getOriginalName(), p.getStoredFile().getFileSize(), p.getStoredFile().getContentType(), p.getStoredFile().getCreatedAt(), p.getStoredFile().getEditedAt(), p.getStoredFile().getOwner().getUsername()))
                .toList();

        return new FolderContentDto(subFolders, files);
    }


}
