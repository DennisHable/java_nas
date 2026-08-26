package cz.dhable.projects.nas.service;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

public interface StorageService {

    /**
     * fyzicky zapíše proud bajtů (stream) ze sítě na určené místo v úložišti;
     * využijeme InputStream, aby se gigabajtové soubory nahrávaly postupně a nezabraly RAM
     */
    void save(String physicalPath, InputStream inputStream) throws IOException;

    /**
     * načte soubor z úložiště jako Spring Resource připravený pro download nebo streamování
     */
    Resource load(String physicalPath) throws MalformedURLException;

    /**
     * fyzicky odstraní soubor z úložiště
     */
    void delete(String physicalPath) throws IOException;
}
