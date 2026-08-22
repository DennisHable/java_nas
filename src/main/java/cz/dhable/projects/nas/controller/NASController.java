package cz.dhable.projects.nas.controller;

import cz.dhable.projects.nas.model.dto.FileDownloadDto;
import cz.dhable.projects.nas.model.dto.FolderContentDto;
import cz.dhable.projects.nas.model.dto.FolderTreeDto;
import cz.dhable.projects.nas.model.entity.Folder;
import cz.dhable.projects.nas.repository.FolderRepository;
import cz.dhable.projects.nas.repository.StoredFileRepository;
import cz.dhable.projects.nas.service.FileService;
import cz.dhable.projects.nas.service.FolderService;
import cz.dhable.projects.nas.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/nas")
public class NASController {

    private final FileService fileService; // správa souborů - uložení, (soft) smazání, stáhnutí
    private final FolderService folderService; // vytvoření, mazání složek

    public NASController(FileService fileService, FolderService folderService, UserService userService, FolderRepository folderRepository, StoredFileRepository fileRepository) {
        this.fileService = fileService;
        this.folderService = folderService;
    }

    /**
     * endpoint pro nahrávání souboru;
     * vstup: binární soubor a volitelné ID složky (kam se má uložit)
     */
    @PostMapping("/files/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file, // soubor samotný
            @RequestParam(value = "folderId", required = false) Long folderId, // volitelná složka kam se uloží
            Authentication auth) throws IOException { // musí být přihlášen, vytáhneme uživatele = vlastník
        // controller pouze předává data a jméno přihlášeného uživatele service
        UUID savedFileId = fileService.uploadFile(file, folderId, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFileId.toString());  // soubor nahrán; vracíme ID
    }

    /**
     * endpoint pro vytvoření nové složky
     */
    @PostMapping("/folders/create")
    public ResponseEntity<String> createFolder(
            @RequestParam("name") String name, // název složky (pouze v db; na disku to je řešeno jinak)
            @RequestParam(value = "parentId", required = false) Long parentId, // nadřazená složka
            Authentication auth) { // přihlášený uživatel = vlastník
        Folder newFolder = folderService.createFolder(name, parentId, auth.getName()); // vytvoření složky; vložení záznamu do db
        return ResponseEntity.status(HttpStatus.CREATED).body(newFolder.getId().toString()); // složka vytvořena; vracíme ID složky
    }


    /**
     * endpoint pro stažení souboru
     * vrací binární proud dat (Resource) a nastavuje hlavičky pro prohlížeč
     */
    @GetMapping("/files/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") UUID fileId, // unikátní identifikátor souboru
                                                 Authentication auth) throws IOException {
        // kontrola práv a načtení binárního souboru z disku
        Resource resource = fileService.downloadFile(fileId, auth.getName());

        // metadata, abychom znali název a content type
        FileDownloadDto fileDetails = fileService.getFileDetailsForDownload(fileId);

        String encodedFilename = URLEncoder.encode(fileDetails.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileDetails.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }


    /**
     * endpoint pro vyhození souboru do koše
     */
    @DeleteMapping("/files/{id}/trash")
    public ResponseEntity<String> moveFileToTrash(@PathVariable("id") UUID fileId, Authentication auth) {
        fileService.moveToFileToTrash(fileId, auth.getName());
        return ResponseEntity.ok("The file was moved to the Bin.");
    }


    /**
     * endpoint pro pohled; zobrazí obsah aktuální složky (soubory i podsložky); čte z DB, zobrazí i soubory/složky z
     * neaktivních (nebo i odpojených disků); pokud je folderId nezdadáno, vrátíme obsah kořenového adresáře NASu
     */
    @GetMapping("/content")
    public ResponseEntity<FolderContentDto> getFolderContent(
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication auth) {
        // vrátí hotová DTO bez rizika LAZY výjimek
        FolderContentDto content = folderService.getFolderContent(folderId, auth.getName());
        return ResponseEntity.ok(content);
    }


    /**
     * zobrazí jednoduchý seznam všech smazaných souborů/složek daného uživatele
     */
    @GetMapping("/trash")
    public ResponseEntity<FolderContentDto> getTrashContent(Authentication auth) {
        return ResponseEntity.ok(folderService.getTrashContent(auth.getName()));
    }

    /**
     * obnovení konkrétního souboru z koše
     */
    @PostMapping("/files/{id}/restore")
    public ResponseEntity<String> restoreFile(@PathVariable("id") UUID fileId, Authentication auth) {
        fileService.restoreFileFromTrash(fileId, auth.getName());
        return ResponseEntity.ok("File was restored");
    }

    /**
     * obnovení celé složky (včetně souborů/složek uvnitř) z koše
     */
    @PostMapping("/folders/{id}/restore")
    public ResponseEntity<String> restoreFolder(@PathVariable("id") Long folderId, Authentication auth) {
        folderService.restoreFolderFromTrash(folderId, auth.getName());
        return ResponseEntity.ok("Folder was restored");
    }

    /**
     * endpoint pro streamování médií
     */
    @GetMapping("/files/stream/{id}")
    public ResponseEntity<ResourceRegion> streamFile(
            @PathVariable("id") UUID fileId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            Authentication auth) throws IOException {

        // řeší práva, načtení z disku i výpočet bajtů
        ResourceRegion region = fileService.streamFileRegion(fileId, rangeHeader, auth.getName());

        // metadata pro zjištění Content-Type (např. video/mp4/...)
        FileDownloadDto fileDetails = fileService.getFileDetailsForDownload(fileId);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType(fileDetails.getContentType()))
                .body(region);
    }


    /**
     * endpoint pro vyhození souboru do koše
     */
    @DeleteMapping("/folders/{id}/trash")
    public ResponseEntity<String> moveFolderToTrash(@PathVariable("id") Long folderId, Authentication auth) {
        folderService.moveFolderToTrash(folderId, auth.getName());
        return ResponseEntity.ok("The folder was moved to the Bin.");
    }


    /**
     * endpoint pro definitivní fyzické smazání souboru z disku i z DB.
     */
    @DeleteMapping("/files/{id}")
    public ResponseEntity<String> permanentlyDeleteFile(@PathVariable("id") UUID fileId, Authentication auth) throws IOException {
        // ověří, zda je soubor v koši a smaže ho z disku i z DB
        fileService.deleteFile(fileId, auth.getName());
        return ResponseEntity.ok("The file was permanently deleted from the disk.");
    }

    /**
     * endpoint pro definitivní fyzické smazání složky (všetně podsložek a souborů uvnitř) z disku i z DB.
     */
    @DeleteMapping("/folders/{id}")
    public ResponseEntity<String> permanentlyDeleteFolder(@PathVariable("id") Long folderId, Authentication auth) throws IOException {
        folderService.permanentlyDeleteFolder(folderId, auth.getName());
        return ResponseEntity.ok("The folder and its contents have been permanently deleted.");
    }

    @PutMapping("/files/update")
    public ResponseEntity<String> updateFile(
            @RequestParam("file") MultipartFile file, // obsah
            @RequestParam("fileId") UUID fileId, // id souboru
            Authentication auth) throws IOException { // přihlášený uživ.

        fileService.updateFileContent(file, fileId, auth.getName());
        return ResponseEntity.ok("The file was sucesfully updated on the disk.");
    }

    @PatchMapping("/files/{id}/rename")
    public ResponseEntity<String> renameFile(@PathVariable("id") UUID fileId,
                                             @RequestParam("newName") String newName,
                                             Authentication auth) {
        fileService.renameFile(fileId, newName, auth.getName());
        return ResponseEntity.ok("The file was renamed");
    }

    @PatchMapping("/files/{id}/move")
    public ResponseEntity<String> moveFile(@PathVariable("id") UUID fileId,
                                           @RequestParam(value = "targetFolderId", required = false) Long targetFolderId,
                                           Authentication auth) {
        fileService.moveFile(fileId, targetFolderId, auth.getName());
        return ResponseEntity.ok("The file was moved");
    }

    @PatchMapping("/folders/{id}/rename")
    public ResponseEntity<String> renameFolder(@PathVariable("id") Long folderId,
                                               @RequestParam("newName") String newName,
                                               Authentication auth) {
        folderService.renameFolder(folderId, newName, auth.getName());
        return ResponseEntity.ok("The folder was renamed");
    }

    @PatchMapping("/folders/{id}/move")
    public ResponseEntity<String> moveFolder(@PathVariable("id") Long folderId,
                                             @RequestParam(value = "targetParentId", required = false) Long targetParentId,
                                                Authentication auth) {
        folderService.moveFolder(folderId, targetParentId, auth.getName());
        return ResponseEntity.ok("The folder was moved");
    }

    /**
     * multi-upload (nahrání více souborů najednou); bere pole souborů najednou
     */
    @PostMapping("/files/upload/multi")
    public ResponseEntity<String> uploadFiles(
            @RequestParam("file") MultipartFile[] files, // pole souborů (multi part )
            @RequestParam(value = "folderId", required = false) Long folderId, // kam; null == root NASu
            Authentication auth) throws IOException {
        // projdeme soubory, které uživatel poslal v poli
        for (MultipartFile file : files) {
            // stejná metoda jako pro 1 soubor (každý soubor dostane své UUID)
            fileService.uploadFile(file, folderId, auth.getName());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("All files have been successfully uploaded.");
    }

    /**
     * strom adresářů v NASu; pouze složky které vlasntí aktuální uživatel
     */
    @GetMapping("/folders/tree")
    public ResponseEntity<List<FolderTreeDto>> getFolderTree(Authentication auth) {
        return ResponseEntity.ok(folderService.getFolderTree(auth.getName()));
    }


}

