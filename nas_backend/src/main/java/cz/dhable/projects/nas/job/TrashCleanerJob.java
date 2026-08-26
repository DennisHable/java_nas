package cz.dhable.projects.nas.job;

import cz.dhable.projects.nas.model.entity.Folder;
import cz.dhable.projects.nas.model.entity.StoredFile;
import cz.dhable.projects.nas.repository.FolderRepository;
import cz.dhable.projects.nas.repository.StoredFileRepository;
import cz.dhable.projects.nas.service.StorageService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling // aktivuje plánovač úloh ve Springu
public class TrashCleanerJob {

    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final StorageService storageService;

    public TrashCleanerJob(StoredFileRepository fileRepository, FolderRepository folderRepository, StorageService storageService) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.storageService = storageService;
    }

    /**
     * spustí se každý den o půlnoci (cron výraz: sekundy minuty hodiny den měsíc rok)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoDeleteOldTrash() {
        LocalDateTime limitDate = LocalDateTime.now().minusDays(30);

        // najdeme a smažeme z db i z disku staré soubory
        List<StoredFile> oldFiles = fileRepository.findByDeletedTrueAndDeletedAtBefore(limitDate);
        for (StoredFile file : oldFiles) {
            try {
                // smazání z DB
                fileRepository.delete(file);
                // fyzický smazání z disku NASu
                storageService.delete(file.getPhysicalPath());
            } catch (IOException e) {
                System.err.println("The old file cannot be physically deleted from the disk: " + file.getPhysicalPath());
            }
        }

        // najít, smazat staré složky
        // složky nemají fyzický soubor na disku, mažeme jen záznam z DB
        List<Folder> oldFolders = folderRepository.findByDeletedTrueAndDeletedAtBefore(limitDate);
        if (!oldFolders.isEmpty()) {
            folderRepository.deleteAll(oldFolders);
        }
    }
}
