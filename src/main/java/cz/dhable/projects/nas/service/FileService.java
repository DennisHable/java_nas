package cz.dhable.projects.nas.service;

import cz.dhable.projects.nas.model.dto.FileDownloadDto;
import cz.dhable.projects.nas.model.entity.*;
import cz.dhable.projects.nas.repository.FolderRepository;
import cz.dhable.projects.nas.repository.SharePermissionRepository;
import cz.dhable.projects.nas.repository.StorageRootRepository;
import cz.dhable.projects.nas.repository.StoredFileRepository;
import cz.dhable.projects.nas.util.StoragePathResolver;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {

    private final UserService userService; // vyhledání uživ. podle jména v db
    private final StorageService storageService; // injektujeme rozhraní (volá se LocalStorageServiceImpl)

    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final SharePermissionRepository sharePermissionRepository;
    private final StorageRootRepository storageRootRepository;
    private UserService userRepository;

    private final int MAX_CNT = 100; // max. počet pokusů na generování UUID

    // private Path rootStoragePath = Paths.get("/home/dhable/nas_storage").toAbsolutePath().normalize(); // Pro začátek pevná cesta, než bude StorageRoot pro ADMINA

    public FileService(StoredFileRepository fileRepository, FolderRepository folderRepository, UserService userService, StorageService storageService, SharePermissionRepository sharePermissionRepository, StorageRootRepository storageRootRepository, UserService userRepository) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.userService = userService;
        this.storageService = storageService;
        this.sharePermissionRepository = sharePermissionRepository;
        this.storageRootRepository = storageRootRepository;
        // vytvoří při startu  automaticky složku pro soubory (pokud neexistuje)
        /*try {
            Files.createDirectories(rootStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("The file storage cannot be initialized.", e);
        }*/
        this.userRepository = userRepository;
    }

    /**
     * transakční nahrání souboru na disk a zápis do DB
     */
    @Transactional
    public UUID uploadFile(MultipartFile file, Long folderId, String userName) throws IOException {
        User owner = userService.findByUsername(userName); // přihlášený uživ. = vlastník
        if (owner == null) throw new RuntimeException("User was not found");

        Folder folder = null; // kořenový adresář NASu
        if (folderId != null) { // složka musí existovat, když tam chceme uložit soubor
            folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Destination folder was not found"));

            // kontrola práva zápisu; musí být vlastník složky, nebo ADMIN, nebo mít SharePermission s canWrite = true
            boolean hasWriteAccess = folder.getOwner().getId().equals(owner.getId()) // vlastník
                    || owner.getRole().name().equals("ADMIN") // admin
                    || sharePermissionRepository.findBySharedWithAndFolder(owner, folder)
                    .map(SharePermission::isCanWrite).orElse(false); // existuje záznam v db v SharePermission s canWrite = true pro tohoto uživatele a tuto složku

            if (!hasWriteAccess) {
                throw new SecurityException("You do not have permission to upload files to this folder");
            }
        }

        // načtení aktivního disku
        StorageRoot activeRoot = storageRootRepository.findByActiveTrue()
                .orElseThrow(() -> new RuntimeException("Critical error: The admin has not set an active disk for data recording."));

        Path rootStoragePath = Paths.get(activeRoot.getBasePath()).toAbsolutePath().normalize();

        // vytvoří při startu  automaticky složku pro soubory (pokud neexistuje)
        try {
            Files.createDirectories(rootStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("The file storage cannot be initialized.", e);
        }

        // generování unikátního fyzického názvu (UUID); pro zamezení kolizí
        UUID fileId = UUID.randomUUID();
        Path physicalPath = StoragePathResolver.resolveShardedPath(rootStoragePath, fileId); // reálná cesta k souboru na disku serveru

        // Smyčka jako pojistka pro případ (scifi) duplicity UUID na disku
        int attempts = 0;
        while (true) {
            if (Files.exists(physicalPath)) { // existuje, tak geenrujeme nové
                attempts++;
                if (attempts >= MAX_CNT)
                    throw new RuntimeException("Critical error: Failed to generate a unique UUID"); // btw TODO logger
                // nastala kolize => generujeme nové UUID a zkusíme to znovu uložit
                fileId = UUID.randomUUID();
                physicalPath = StoragePathResolver.resolveShardedPath(rootStoragePath, fileId);
            } else {
                break; // unikátní UUID souboru
            }
        }

        // přes definované rozhraní, nahráváme soubory -- efektivní streaming RAM bajtů na disk
        // bez REPLACE_EXISTING => pokud soubor existuje, ta to vyhodí chybu
        storageService.save(physicalPath.toString(), file.getInputStream()); // fyzické uložení souboru na disk na serveru


        // entita pro db
        StoredFile storedFile = new StoredFile(
                fileId, // UUID souboru
                file.getOriginalFilename(), // původní název souboru (jak ho nahrál uživatel)
                physicalPath.toString(), // cesta na disku serveru
                file.getSize(), // velikost souboru
                file.getContentType(), // typ souboru
                folder, // složka ve které se soubor nachází
                owner // majitel souboru; ten kdo ho nahrál
        );

        fileRepository.saveAndFlush(storedFile); // uložení do db; okamžitý zápis

        return fileId;
    }

    /**
     * vyhození souboru do Koše (jen Soft-delete; soubor z disku nemizí jen se nastaví příznak; delete proběhne déle)
     */
    @Transactional
    public void moveToFileToTrash(UUID fileId, String userName) {
        User user = userService.findByUsername(userName); // přihlášený uživ. = vlastník
        if (user == null) throw new RuntimeException("User was not found");

        StoredFile storedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File was not found"));

        // pouze vlastník nebo ADMIN
        if (!storedFile.getOwner().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("You do not have permission to delete this file");
        }

        storedFile.setDeleted(true); // označíme jako smazaný
        storedFile.setDeletedAt(LocalDateTime.now()); // datum kdy byl "smazán" (označen za smazaný)

        // uložíme změnu flagu do DB
        fileRepository.saveAndFlush(storedFile);
    }

    /**
     * reálné smazání z koše (fyzické smazání z disku i z DB)
     */
    @Transactional
    public void deleteFile(UUID fileId, String userName) throws IOException {
        User user = userService.findByUsername(userName); // přihlášený uživ. = vlastník
        if (user == null) throw new RuntimeException("User was not found");

        StoredFile storedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File was not found"));

        if (!storedFile.getOwner().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new SecurityException("You do not have permission to permanently delete this file");
        }

        if (!storedFile.isDeleted()) {
            throw new IllegalStateException("The file cannot be permanently deleted until it is in the Bin");
        }

        // smažeme z DB
        fileRepository.delete(storedFile);

        // fyzické smazání z disku přes rozhraní
        storageService.delete(storedFile.getPhysicalPath());
    }


    /**
     * načtení souboru z disku pro stažení (s kontrolou práv); transakční - buď celé nebo nic
     */
    @Transactional(readOnly = true)
    public Resource downloadFile(UUID fileId, String userName) throws MalformedURLException {
        // TODO možná kontrola jestli disk s tímto souborem není ADMINEM odebrán (přímé download přes API; frontend to nepodporuje)

        User user = userService.findByUsername(userName); // přihlášený uživ. = vlastník
        if (user == null) throw new RuntimeException("User was not found");

        // vytáhneme metadata z db (původní název souboru)
        StoredFile storedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File was not found."));

        // kontrola přímého vlastnictví - vlastník souboru nebo uživatel s rolí ADMIN
        boolean hasAccess = storedFile.getOwner().getId().equals(user.getId()) // porovnání id majitele a aktuálního uživatele
                || user.getRole().name().equals("ADMIN"); // má roli ADMIN

        // kontrola přímého sdílení daného souboru
        if (!hasAccess) {
            // existuje záznam v tabulce SharePermission, kde má tento uživ. oprávnění aspoň ke čtení tohoto souboru
            hasAccess = sharePermissionRepository.findBySharedWithAndStoredFile(user, storedFile).isPresent();
        }

        // kontrola  sdílení nadřazené složky (pokud v nějaké složce leží, tj. není v kořenové složce NASu)
        if (!hasAccess && storedFile.getFolder() != null) {
            // má udělené právo ke čtení v daném adresáři
            hasAccess = sharePermissionRepository.findBySharedWithAndFolder(user, storedFile.getFolder()).isPresent();
        }

        if (!hasAccess) {
            throw new SecurityException("You do not have permission to download this file.");
        }

        Path physicalPath = Path.of(storedFile.getPhysicalPath()); // celá reálná cesta k souboru na disku serveru

        // stáhnutí z disku
        return storageService.load(physicalPath.toString());
    }


    @Transactional
    public FileDownloadDto getFileDetailsForDownload(UUID fileId) {
        // vytáhneme metadata z db (původní název souboru)
        StoredFile storedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File was not found."));

        return new FileDownloadDto(storedFile.getOriginalName(), storedFile.getContentType());
    }

    /**
     * obnova souboru z koše, včetně adresářů ve kterých se nacházel
     */
    @Transactional
    public void restoreFileFromTrash(UUID fileId, String currentUsername) {
        StoredFile storedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File was not found"));

        if (!storedFile.getOwner().getUsername().equals(currentUsername)) {
            throw new SecurityException("You do not have permission to restore this file.");
        }

        // rekurzivní obnova nadřazených složek ve kterých soubor ležel
        if (storedFile.getFolder() != null && storedFile.getFolder().isDeleted()) { // pokud není v rootu NASu a nadřazený adresář je v koši
            restoreFolderAndParents(storedFile.getFolder()); // obnov
        }

        storedFile.setDeleted(false);
        storedFile.setDeletedAt(null);
        fileRepository.save(storedFile);
    }

    // vytáhne z koše danou složku i všechny její případné nadřazené složky v koši (až do rootu NASu)
    private void restoreFolderAndParents(Folder folder) {
        folder.setDeleted(false); // změna flagu
        folder.setDeletedAt(null);
        folderRepository.save(folder); // propis do DB

        // pokud má i tato složka nadřazenou složku v koši, tak ji obnovíme taky
        if (folder.getParent() != null && folder.getParent().isDeleted()) {
            restoreFolderAndParents(folder.getParent());
        }
    }


    @Transactional(readOnly = true)
    public ResourceRegion streamFileRegion(UUID fileId, String rangeHeader, String currentUsername) throws IOException {
        // ověříme uživatele, práva (přes downloadFile, která hází výjimky při neoprávněném přístupu)
        Resource resource = this.downloadFile(fileId, currentUsername);
        long contentLength = resource.contentLength();

        // definujeme velikost jednoho bloku dat (1 MB buffer pro plynulé streamování)
        long chunkSize = 1024 * 1024;

        // spočítáme rozsah bajtů na základě HTTP hlavičky Range
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            try {
                List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
                if (!ranges.isEmpty()) {
                    HttpRange range = ranges.getFirst();
                    long start = range.getRangeStart(contentLength);
                    long end = range.getRangeEnd(contentLength);
                    long rangeLength = Math.min(chunkSize, end - start + 1);
                    return new ResourceRegion(resource, start, rangeLength);
                }
            } catch (IllegalArgumentException e) {
                // pokud prohlížeč pošle neplatný rozsah (např. mimo velikost souboru)
                throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Invalid file range");
            }
        }

        // pokud prohlížeč neposlal Range (začátek přehrávání), pošleme první megabajt od nuly
        return new ResourceRegion(resource, 0, Math.min(chunkSize, contentLength));
    }


    @Transactional
    public void updateFileContent(MultipartFile file, UUID fileId, String currentUsername) throws IOException {
        StoredFile storedFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File was not found"));

        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) throw new RuntimeException("User was not found");

        // Kontrola práv zápisu: Vlastník, ADMIN, nebo SharePermission s canWrite = true
        boolean hasWriteAccess = storedFile.getOwner().getId().equals(currentUser.getId())
                || currentUser.getRole().name().equals("ADMIN")
                || sharePermissionRepository.findBySharedWithAndStoredFile(currentUser, storedFile) // záznm, kde nějaký uživ. sdílí s tímto tento soubor
                .map(SharePermission::isCanWrite).orElse(false); // musí mít i právo k zápisu

        if (!hasWriteAccess) {
            throw new SecurityException("You do not have permission to update this file.");
        }

        // přepíš stávající soubor na disku přes to rozhraní
        storageService.save(storedFile.getPhysicalPath(), file.getInputStream());

        // aktualizace velikosti a čas editace v DB
        storedFile.setFileSize(file.getSize());
        storedFile.setEditedAt(LocalDateTime.now());
        fileRepository.save(storedFile);
    }


    @Transactional
    public void renameFile(UUID fileId, String newName, String currentUsername) {
        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File was not found"));

        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) throw new RuntimeException("User was not found");


        // Kontrola práv zápisu (Vlastník, ADMIN, nebo SharePermission s canWrite = true)
        boolean hasWriteAccess = file.getOwner().getId().equals(currentUser.getId())
                || currentUser.getRole().name().equals("ADMIN")
                || sharePermissionRepository.findBySharedWithAndStoredFile(currentUser, file)
                .map(SharePermission::isCanWrite).orElse(false);

        if (!hasWriteAccess) {
            throw new SecurityException("You do not have permission to rename this file.");
        }

        file.setOriginalName(newName); // přejmenování v DB; na disku NASu jsou jen UUID
        file.setEditedAt(LocalDateTime.now()); // čas editace
        fileRepository.save(file); // propis do DB
    }

    /**
     * přesun souboru (fileId) do nové složky (targetFolderId); uživatelem (currentUsername) pokud má oprávnění
     */
    @Transactional
    public void moveFile(UUID fileId, Long targetFolderId, String currentUsername) {
        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File was not found"));

        User currentUser = userRepository.findByUsername(currentUsername);
        if (currentUser == null) throw new RuntimeException("User was not found");

        Folder targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepository.findById(targetFolderId)
                    .orElseThrow(() -> new RuntimeException("Destination folder was not found"));
        }

        // Kontrola práv zápisu (Vlastník, ADMIN, nebo SharePermission s canWrite = true)
        boolean hasWriteAccess = file.getOwner().getId().equals(currentUser.getId())
                || currentUser.getRole().name().equals("ADMIN")
                || sharePermissionRepository.findBySharedWithAndStoredFile(currentUser, file)
                .map(SharePermission::isCanWrite).orElse(false);

        if (!hasWriteAccess) {
            throw new SecurityException("You do not have permission to rename this file.");
        }

        file.setFolder(targetFolder); // přenastavení rodičovského adresáře
        file.setEditedAt(LocalDateTime.now()); // čas editu
        fileRepository.save(file);
    }


    public void streamZipArchive(List<UUID> fileIds, OutputStream outputStream) { // máme paměťový stream, do kterého budeme zapisovat data ZIPu
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) { // bere data a za běhu z nich dělá ZIP; posílá ho do přímo do outputStreamu, který se sem předal v parametru, přes ten to jde přímo ke klientovi

            // projdeme všechna zaslaná ID souborů
            for (UUID fileId : fileIds) {
                // Načteme soubor z DB, abychom znali jeho originální název a fyzickou cestu na disku
                StoredFile storedFile = fileRepository.findById(fileId)
                        .orElseThrow(() -> new RuntimeException("Soubor s ID " + fileId + " neexistuje"));

                Path filePath = Paths.get(storedFile.getPhysicalPath()); // cesta na disku

                // Kontrola, zda soubor reálně existuje na pevném disku serveru
                if (!Files.exists(filePath)) {
                    // klidně jen přeskočíme
                    continue;
                }

                // vytvoříme nový záznam (záznam v tabulce obsahu ZIPu) s originálním názvem souboru; hlavička konkrétního souboru (název, datum vytvoření, velikost, ...)
                ZipEntry zipEntry = new ZipEntry(storedFile.getOriginalName());
                zos.putNextEntry(zipEntry); // značí začátek toho souboru ve streamu

                // otevře soubor, čte po částech do bufferu (z pevného disku serveru), zapíše do ZIP streamu, ten to pošle; přečte další data a opakuje
                Files.copy(filePath, zos);

                // Uzavřeme záznam pro tento konkrétní soubor, ukončení souboru ve streamu
                zos.closeEntry();
            }

            // před dokončením metody musíme vynutit zapsání všech zbývajících dat do ZIPu
            zos.finish();
        } catch (IOException e) {
            throw new RuntimeException("Chyba při generování ZIP archivu na serveru", e);
        }
    }
}