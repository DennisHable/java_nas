package cz.dhable.projects.nas.controller;

import cz.dhable.projects.nas.model.dto.SystemStatsDto;
import cz.dhable.projects.nas.service.MonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * vrátí statistiky CPU, RAM a disku
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')") // tato anotace zajistí, že sem nikdo kromě ADMINA nevleze
    public ResponseEntity<SystemStatsDto> getSystemStats() {
        return ResponseEntity.ok(monitoringService.getSystemStats());
    }
}
