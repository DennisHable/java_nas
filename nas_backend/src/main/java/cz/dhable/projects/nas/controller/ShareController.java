package cz.dhable.projects.nas.controller;

import cz.dhable.projects.nas.model.dto.FolderContentDto;
import cz.dhable.projects.nas.model.dto.SharePermissionResponseDto;
import cz.dhable.projects.nas.model.dto.ShareRequestDto;
import cz.dhable.projects.nas.service.ShareService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nas/share")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * endpoint pro udělení nebo úpravu (read/write) přístupových práv k souboru/složce
     */
    @PostMapping
    public ResponseEntity<String> shareResource(@RequestBody ShareRequestDto dto, Authentication auth) {
        shareService.shareResource(dto, auth.getName());
        return ResponseEntity.ok("Permission sharing has been successfully set up");
    }

    /**
     * vrátí list souborů a složek co s někým akt. uživ. sdílí
     */
    @GetMapping("/i-share")
    public ResponseEntity<List<SharePermissionResponseDto>> getThingsIShare(Authentication auth) {
        return ResponseEntity.ok(shareService.getThingsIShare(auth.getName()));
    }

    /**
     * odebere opravnění
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> revokePermission(@PathVariable("id") Long permissionId, Authentication auth) {
        shareService.revokePermission(permissionId, auth.getName());
        return ResponseEntity.ok("Sharing has been successfully cancelled.");
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<FolderContentDto> getThingsSharedWithMe(Authentication auth) {
        return ResponseEntity.ok(shareService.getThingsSharedWithMe(auth.getName()));
    }


}
