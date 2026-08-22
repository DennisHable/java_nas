package cz.dhable.projects.nas.service.impl;

import cz.dhable.projects.nas.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalStorageServiceImpl implements StorageService {

    @Override
    public void save(String physicalPath, InputStream inputStream) throws IOException {
        Path targetPath = Paths.get(physicalPath);

        // Java se podívá na sharded cestu (např. .../4a/7b/)
        // a pokud tyto podsložky ještě neexistují, automaticky je na disku (SSD) NASu vytvoří.
        Files.createDirectories(targetPath.getParent());

        // zkopíruje data z input streamu přímo do souboru na disku; ukládá se přes buffer v RAM na disk, tedy
        // nenačítá se celý soubor do RAM; je synchronní blokující -- nahrávání více souborů, ale automaticky řeší
        // Tomcat přes vlákna (Thread Pool), kdy pro příchozí požadavaek použije jiné vlákno
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING); // StandardCopyOption.REPLACE_EXISTING -- přepis souboru, pokud by existoval; edit
    }

    @Override
    public Resource load(String physicalPath) throws MalformedURLException {
        Path filePath = Paths.get(physicalPath); // cesta na disku serveru
        // objekt cesty -> URI formát; vytvoří objekt reprezentující ten soubor, dle té cesty
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) { // soubor existuje; apka má právo ke čtení
            return resource; // vrátí načtený soubor
        } else {
            throw new RuntimeException("The file does not exist on the disk or is not readable: " + physicalPath);
        }
    }

    @Override
    public void delete(String physicalPath) throws IOException {
        Path filePath = Paths.get(physicalPath);
        // Fyzicky smaže soubor z disku, pokud tam existuje
        Files.deleteIfExists(filePath);
    }
}
