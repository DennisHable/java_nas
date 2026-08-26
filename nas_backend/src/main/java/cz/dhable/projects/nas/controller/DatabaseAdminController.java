package cz.dhable.projects.nas.controller;

import cz.dhable.projects.nas.service.DatabaseBackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/nas/database")
@PreAuthorize("hasRole('ADMIN')") // metody může volat jen ADMIN
// (Spring to ví na základě přihlášeného uživatele v paměti během aktuálního požadavku;
// dle JSESSIONID najde uživatele v RAM serveru (příslušnou HttpSession - data jednoho uživatele) vytáhne
// SecurityContext (identifikace uživatele) dá ho do SecurityContextHolder pro aktuální vlákno); jinak chyba
public class DatabaseAdminController {


    private final DatabaseBackupService backupService;

    public DatabaseAdminController(DatabaseBackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Exportuje vybrané tabulky (v požadavku) do jednoho JSON souboru přes stream.
     */
    @PostMapping("/export")
    public void exportDatabase(
            @RequestBody(required = false) List<String> tables,
            HttpServletResponse response) {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nas_db_backup.json\"");

        try {
            // Předáme výstupní proud odpovědi (tam se budou zapisovat data pro klienta) přímo do příslušné služby
            backupService.exportDatabase(tables, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException("Chyba při generování exportu databáze", e);
        }
    }

    /**
     * Vezmeme nahraný JSON soubor se zálohou DB a provede import/obnovu dat do té DB.
     */
    @PostMapping("/import")
    public ResponseEntity<String> importDatabase(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Nahraný soubor je prázdný.");
        }

        try {
            backupService.importDatabase(file.getInputStream());
            return ResponseEntity.ok("Import databáze proběhl úspěšně.");
        } catch (Exception e) {
            // zychytí GlobalExceptionHandler (s anot. @RestControllerAdvice) a přeloží to na Bad Request
            throw new RuntimeException("Import zálohy selhal: " + e.getMessage(), e);
        }
    }
}
