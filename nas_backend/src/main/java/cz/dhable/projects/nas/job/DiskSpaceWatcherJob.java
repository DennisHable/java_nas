package cz.dhable.projects.nas.job;

import cz.dhable.projects.nas.repository.StorageRootRepository;
import cz.dhable.projects.nas.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//@Component
// @EnableScheduling
public class DiskSpaceWatcherJob {
/*
    private final StorageRootRepository storageRootRepository;
    private final EmailService emailService;

    @Value("${app.nas.admin-email}")
    private String adminEmail; // načte e-mail admina

    // aby systém neposílal e-mail každou hodinu, pokud je disk plný
    private boolean emailAlreadySent = false;

    public DiskSpaceWatcherJob(StorageRootRepository storageRootRepository, EmailService emailService) {
        this.storageRootRepository = storageRootRepository;
        this.emailService = emailService;
    }

    /**
     * spustí se každou hodinu (na začátku hodiny: 0. minuta, 0. sekunda)
     /
    @Scheduled(cron = "0 0 * * * *")
    public void checkDiskSpace() {
        // zjistíme, na jaký disk se aktuálně nahrává
        storageRootRepository.findByActiveTrue().ifPresent(activeRoot -> {
            try {
                Path path = Paths.get(activeRoot.getBasePath()).toAbsolutePath().normalize();
                FileStore store = Files.getFileStore(path);

                long totalSpace = store.getTotalSpace();
                long usableSpace = store.getUsableSpace();
                long usedSpace = totalSpace - usableSpace;

                // spočítáme procentuální zaplnění disku
                double usedPercent = ((double) usedSpace / totalSpace) * 100;

                // pokud zaplnění překročí 90 % a e-mail jsme ještě neposílali
                if (usedPercent >= 90.0) {
                    if (!emailAlreadySent) {
                        String subject = "VAROVÁNÍ: Disk NASu [" + activeRoot.getDiskName() + "] je téměř plný!";
                        String text = "Vážený administrátore,\n\n" +
                                "Kapacita aktivního disku pro zápis překročila kritickou hranici.\n\n" +
                                "Detaily disku:\n" +
                                "- Název: " + activeRoot.getDiskName() + "\n" +
                                "- Cesta v OS: " + activeRoot.getBasePath() + "\n" +
                                "- Celková kapacita: " + (totalSpace / (1024 * 1024 * 1024)) + " GB\n" +
                                "- Aktuální zaplnění: " + Math.round(usedPercent) + " %\n\n" +
                                "Prosím, připojte nový úložný prostor nebo promažte Systémový koš.\n\n" +
                                "Váš NAS backend.";

                        emailService.sendSimpleEmail(adminEmail, subject, text);
                        emailAlreadySent = true; // Uzamkneme posílání, dokud se situace nevyřeší
                    }
                } else {
                    // pokud klesne zaplnění pod 90 % (admin disk promazal; uživ. něco smazali), resetujeme pojistku
                    emailAlreadySent = false;
                }

            } catch (IOException e) {
                System.err.println("The disk monitor could not read the folder: " + activeRoot.getBasePath());
            }
        });
    }*/
}
