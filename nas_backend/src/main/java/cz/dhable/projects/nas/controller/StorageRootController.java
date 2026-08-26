package cz.dhable.projects.nas.controller;

import cz.dhable.projects.nas.model.dto.StorageRootRequestDto;
import cz.dhable.projects.nas.model.entity.StorageRoot;
import cz.dhable.projects.nas.service.StorageRootService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/disks")
@PreAuthorize("hasRole('ADMIN')") // celý kontrolér je přístupný jen pro ADMINA
public class StorageRootController {

    private final StorageRootService storageRootService;

    public StorageRootController(StorageRootService storageRootService) {
        this.storageRootService = storageRootService;
    }

    @GetMapping
    public ResponseEntity<List<StorageRoot>> getAllDisks() {
        return ResponseEntity.ok(storageRootService.getAllDisks());
    }

    @PostMapping
    public ResponseEntity<StorageRoot> addDisk(@RequestBody StorageRootRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storageRootService.addDisk(dto));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<String> activateDisk(@PathVariable("id") Long diskId) {
        storageRootService.activateDisk(diskId);
        return ResponseEntity.ok("Disk has been successfully activated for recording.");
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateDisk(@PathVariable("id") Long diskId) {
        storageRootService.deactivateDisk(diskId);
        return ResponseEntity.ok("Disk has been successfully deactivated for recording.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDisk(@PathVariable("id") Long diskId) {
        storageRootService.removeDisk(diskId);
        return ResponseEntity.ok("Disk has been successfully removed for recording.");
    }


}
