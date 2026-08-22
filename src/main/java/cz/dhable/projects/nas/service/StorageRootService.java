package cz.dhable.projects.nas.service;

import cz.dhable.projects.nas.model.dto.StorageRootRequestDto;
import cz.dhable.projects.nas.model.entity.StorageRoot;
import cz.dhable.projects.nas.repository.StorageRootRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StorageRootService {

    private final StorageRootRepository storageRootRepository;

    public StorageRootService(StorageRootRepository storageRootRepository) {
        this.storageRootRepository = storageRootRepository;
    }

    /**
     * vrátí seznam všech disků pro tabulku v administraci na frontendu
     */
    @Transactional(readOnly = true)
    public List<StorageRoot> getAllDisks() {
        return storageRootRepository.findAll();
    }

    /**
     * přidání nového disku do systému
     */
    @Transactional
    public StorageRoot addDisk(StorageRootRequestDto dto) {
        if (storageRootRepository.existsByBasePath(dto.getBasePath())) {
            throw new IllegalArgumentException("A disk with this path already exists in the system.");
        }
        return storageRootRepository.save(new StorageRoot(dto.getBasePath(), dto.getDiskName())); // uložení disku do DB
    }

    /**
     * přepnutí aktivního disku pro nahrávání
     */
    @Transactional
    public void activateDisk(Long diskId) {
        StorageRoot targetDisk = storageRootRepository.findById(diskId)
                .orElseThrow(() -> new RuntimeException("Disk was not found"));

        // všechny disky v DB přepneme na false
        List<StorageRoot> allDisks = storageRootRepository.findAll();
        for (StorageRoot disk : allDisks) {
            disk.setActive(false);
        }

        // pouze vybraný disk aktivujeme
        targetDisk.setActive(true);
        storageRootRepository.saveAll(allDisks);
    }

    @Transactional
    public void deactivateDisk(Long diskId) {
        StorageRoot targetDisk = storageRootRepository.findById(diskId)
                .orElseThrow(() -> new RuntimeException("Disk was not found"));

        // disk pouze vypneme; pro zápis v systému v tu chvíli nebude aktivní
        // nebude aktivní nic pokud admin jiný disk neaktivuje a byl to jediný aktivní disk
        targetDisk.setActive(false);
        storageRootRepository.save(targetDisk);
    }

    @Transactional
    public void removeDisk(Long diskId) {
        StorageRoot targetDisk = storageRootRepository.findById(diskId)
                .orElseThrow(() -> new RuntimeException("Disk was not found"));

        // aktivní disk pro zápis smazat nepůjde
        if (targetDisk.isActive()) {
            throw new RuntimeException("This disk cannot be removed because it is set as active.");
        }

        storageRootRepository.delete(targetDisk);
    }
}

