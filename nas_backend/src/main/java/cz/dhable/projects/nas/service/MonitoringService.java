package cz.dhable.projects.nas.service;

import cz.dhable.projects.nas.model.dto.DiskStatsDto;
import cz.dhable.projects.nas.model.dto.SystemStatsDto;
import com.sun.management.OperatingSystemMXBean;
import cz.dhable.projects.nas.model.entity.StorageRoot;
import cz.dhable.projects.nas.repository.StorageRootRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class MonitoringService {

    private final StorageRootRepository storageRootRepository;

    public MonitoringService(StorageRootRepository storageRootRepository) {
        this.storageRootRepository = storageRootRepository;
    }

    // cesta k NAS úložišti – z ní zjistíme volné místo na disku
    // private final Path storagePath = Paths.get("/home/dhable/nas_storage").toAbsolutePath().normalize();

    public SystemStatsDto getSystemStats() {
        // získáme přístup k pokročilému managementu OS v Javě
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        // monitoring CPU
        // vrací hodnotu od 0.0 do 1.0 (vytížení procesoru celým systémem), *100 pro procenta
        double cpuUsage = osBean.getCpuLoad() * 100;
        if (cpuUsage < 0) cpuUsage = 0; // ošetření pro případ, že systém hodnotu zrovna nestihl spočítat

        // zaokrouhlíme využití CPU na dvě desetinná místa
        cpuUsage = Math.round(cpuUsage * 100.0) / 100.0;

        // monitoring RAM; nevhodný, vrací využití paměti v kontejneru
        // long ramTotal = osBean.getTotalMemorySize();
        // long ramFree = osBean.getFreeMemorySize();
        // long ramUsed = ramTotal - ramFree;

        long ramTotal = 0;
        long ramFree = 0;

        try {
            // přečteme systémový soubor paměti v Linuxu
            List<String> lines = Files.readAllLines(Paths.get("/proc/meminfo"));
            long memTotalKb = 0;
            long memAvailableKb = 0;

            for (String line : lines) {
                if (line.startsWith("MemTotal:")) {
                    memTotalKb = Long.parseLong(line.replaceAll("[^0-9]", ""));
                }
                if (line.startsWith("MemAvailable:")) {
                    // MemAvailable je lepší než MemFree, protože započítává i vyrovnávací paměť (cache)
                    memAvailableKb = Long.parseLong(line.replaceAll("[^0-9]", ""));
                    break; // vše máme
                }
            }

            // převod kilobajtů z Linuxu na bajty pro DTO
            ramTotal = memTotalKb * 1024;
            ramFree = memAvailableKb * 1024;

        } catch (Exception e) {
            // nouzový fallback pro případ, že by kód běžel třeba na Windows (kde /proc/meminfo neexistuje)
            ramTotal = osBean.getTotalMemorySize();
            ramFree = osBean.getFreeMemorySize();
        }

        long ramUsed = ramTotal - ramFree;


        /*
        // monitoring disku
        long diskTotal = 0;
        long diskFree = 0;
        long diskUsed = 0;

        try {
            // Java NIO se podívá přímo na souborový systém (Ext4) dané složky
            FileStore store = Files.getFileStore(storagePath);
            diskTotal = store.getTotalSpace();
            diskFree = store.getUsableSpace(); // getUsableSpace bere v potaz i práva uživatele Linuxu
            diskUsed = diskTotal - diskFree;
        } catch (IOException e) {
            // pokud by složka neexistovala, necháme nuly, ale zalogujeme chybu
            System.err.println("Unable to read disk status: " + e.getMessage());
        }

        return new SystemStatsDto(
                cpuUsage,
                ramTotal, ramUsed, ramFree,
                diskTotal, diskUsed, diskFree
        );*/

        // monitoring disků v DB
        List<DiskStatsDto> diskStatsList = new ArrayList<>();
        List<StorageRoot> allRoots = storageRootRepository.findAll(); // z db vytáhneme všechny aktivní/neaktivní disky

        for (StorageRoot root : allRoots) {
            long diskTotal = 0;
            long diskFree = 0;
            long diskUsed = 0;

            try {
                Path path = Paths.get(root.getBasePath()).toAbsolutePath().normalize();
                // Java NIO se podívá přímo na souborový systém (Ext4) dané složky
                FileStore store = Files.getFileStore(path);
                diskTotal = store.getTotalSpace();
                diskFree = store.getUsableSpace(); // getUsableSpace bere tedy v potaz i práva uživatele Linuxu
                diskUsed = diskTotal - diskFree;
            } catch (IOException e) {
                // pokud by složka neexistovala, necháme nuly, ale zalogujeme chybu
                System.err.println("Unable to read disk: " + root.getBasePath() + " status: " + e.getMessage());
            }

            diskStatsList.add(new DiskStatsDto(
                    root.getDiskName(),
                    root.getBasePath(),
                    root.isActive(),
                    diskTotal, diskUsed, diskFree
            ));
        }

        return new SystemStatsDto(cpuUsage, ramTotal, ramUsed, ramFree, diskStatsList);
    }

}
