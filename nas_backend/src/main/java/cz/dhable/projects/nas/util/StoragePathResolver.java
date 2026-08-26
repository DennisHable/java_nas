package cz.dhable.projects.nas.util;

import java.nio.file.Path;
import java.util.UUID;

public class StoragePathResolver {

    /**
     * vezme základní cestu úložiště a vygenerované UUID; vytvoří adresářovou strukturu v NASu, z několika znaků UUID
     * Například:
     * Base (základní cesta k tomu NAS adresáří): /home/admin/nas_storage
     * UUID: 4a7b2e11-88f2-4bc3...
     * Výsledek: /home/admin/nas_storage/4a/7b/4a7b2e11-88f2-4bc3...
     */
    public static Path resolveShardedPath(Path basePath, UUID fileId) {
        String uuidStr = fileId.toString();

        // první 2 znaky (např. "4a")
        String part1 = uuidStr.substring(0, 2);

        // další 2 znaky (např. "7b")
        String part2 = uuidStr.substring(2, 4);

        // spojíme basePath s podsložkami a samotným názvem souboru; když složky neexistují, tak se vytvoří
        // jinak se do nich rovnou zapíše nový soubor
        return basePath.resolve(part1).resolve(part2).resolve(uuidStr);
    }
}
