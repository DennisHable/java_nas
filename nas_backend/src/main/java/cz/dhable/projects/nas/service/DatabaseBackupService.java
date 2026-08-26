package cz.dhable.projects.nas.service;

import com.fasterxml.jackson.databind.SerializationFeature;
import cz.dhable.projects.nas.model.entity.*;
import cz.dhable.projects.nas.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DatabaseBackupService {
    private final UserRepository userRepository;

    private final FolderRepository folderRepository;

    private final StoredFileRepository fileRepository;

    private final SharePermissionRepository sharePermissionRepository;

    private final StorageRootRepository storageRootRepository;

    private final EntityManager entityManager;

    // sdílená instance Jackson mapperu; "překladač" mezi Javou a JSONem (serializace a deserializace zpět)
    private final ObjectMapper objectMapper = new ObjectMapper()
            // Jackson neumí pracovat s novějšími třídami pro čas (díky tomu modulu s nimi může Jackson pracovat); bez něj by to selhalo
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            // Zápis data jako čitelný text (třeba jako "2026-08-25T20:00:00") namísto pole čísel
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            // řeší pády s Lazy Loadingem - když Hibernate vytáhne entitu, tak ty provázaný entity se nevytahují hned (hodí tam prázdný Hibernate Proxy; zástupný objekt); (Jackson neselže a udělá z něj udělá prázný JSON objekt)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            // naopak, když někdo oedituje JSON soubor a bude tam atribut který ten objekt nemá, tak Jackson nespadne (bude to ignorovat); kvůli zpětné kompatibilitě
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public DatabaseBackupService(UserRepository userRepository, FolderRepository folderRepository, StoredFileRepository fileRepository, SharePermissionRepository sharePermissionRepository, StorageRootRepository storageRootRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.sharePermissionRepository = sharePermissionRepository;
        this.storageRootRepository = storageRootRepository;
        this.entityManager = entityManager;
    }

    /**
     * Streamuje vybraná data z repozitářů přímo do síťového OutputStreamu.
     */
    @Transactional(readOnly = true)
    public void exportDatabase(List<String> tables, OutputStream outputStream) throws IOException {
        // Pokud parametr chybí nebo obsahuje "all", vyexportujeme vše
        boolean exportAll = tables == null || tables.isEmpty() || tables.contains("all");

        // vytvoříme kořenový uzel JSON struktury; nový prázdný JSON objekt pro přidávání dat + rovnou metadata
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("version", "1.0");
        rootNode.put("exportedAt", System.currentTimeMillis());

        // uživatelé
        if (exportAll || tables.contains("users")) {
            List<User> users = userRepository.findAll(); // načte list všech záznamů z tabulky
            // převedeme Java entity na JSON uzly
            JsonNode usersNode = objectMapper.valueToTree(users);

            // smazání hashů hesel z exportu; neřeší se ...
            /* for (JsonNode user : usersNode) {
                if (user instanceof ObjectNode) {
                    ((ObjectNode) user).remove("passwordHash");
                }
            }*/

            rootNode.set("users", usersNode); // přidání do výsledného JSONu pod klíč users
        }

        // složky
        if (exportAll || tables.contains("folders")) {
            List<Folder> folders = folderRepository.findAll();
            JsonNode foldersNode = objectMapper.valueToTree(folders);

            for(JsonNode folderNode : foldersNode) { // pro jednotlivé složky v JSONu
                if(folderNode instanceof ObjectNode) { // JsonNode je immutable; ObjectNode je třída, která reprezentuje JSON objekt (klíč hodnota) není immutable
                    JsonNode ownerNode = folderNode.get("owner"); // položka dle klíče v JSONu
                    if(ownerNode != null) { // budeme upravovat přímo ten JSON
                        ((ObjectNode) folderNode).put("ownerId", ownerNode.get("id") != null ? ownerNode.get("id").asLong() : null);
                        ((ObjectNode) folderNode).remove("owner");
                    }
                }
            }

            rootNode.set("folders", foldersNode);
        }

        // soubory
        if (exportAll || tables.contains("stored_files")) {
            List<StoredFile> files = fileRepository.findAll();
            JsonNode filesNode = objectMapper.valueToTree(files);

            for(JsonNode fileNode : filesNode) {
                if(fileNode instanceof ObjectNode) {
                    JsonNode folderNode = fileNode.get("folder");
                    if(folderNode != null) {
                        ((ObjectNode) fileNode).put("folderId", folderNode.get("id") != null ? folderNode.get("id").asLong() : null);
                        ((ObjectNode) fileNode).remove("folder");
                    }

                    JsonNode ownerNode = fileNode.get("owner");
                    if(ownerNode != null) {
                        ((ObjectNode) fileNode).put("ownerId", ownerNode.get("id") != null ? ownerNode.get("id").asLong() : null);
                        ((ObjectNode) fileNode).remove("owner");
                    }
                }
            }

            rootNode.set("stored_files", filesNode);
        }

        // sdílení
        if (exportAll || tables.contains("share_permissions")) {
            List<SharePermission> shares = sharePermissionRepository.findAll();
            JsonNode sharesNode = objectMapper.valueToTree(shares);


            for (JsonNode share : sharesNode) {
                if (share instanceof ObjectNode) {
                    ObjectNode shareObj = (ObjectNode) share;

                    // uživatel, kterému se sdílí
                    JsonNode userNode = shareObj.get("sharedWith"); // název proměnné v entitě
                    if (userNode != null && !userNode.isNull()) {
                        shareObj.put("sharedWithUserId", userNode.get("id").asLong());
                        shareObj.remove("sharedWith");
                    }

                    // sdílená složka
                    JsonNode folderNode = shareObj.get("folder");
                    if (folderNode != null && !folderNode.isNull()) {
                        shareObj.put("folderId", folderNode.get("id").asLong());
                        shareObj.remove("folder");
                    }

                    // sdílený soubor
                    JsonNode fileNode = shareObj.get("storedFile");
                    if (fileNode != null && !fileNode.isNull()) {
                        shareObj.put("storedFileId", fileNode.get("id").asText());
                        shareObj.remove("storedFile");
                    }
                }
            }


            rootNode.set("share_permissions", sharesNode);
        }

        // disky
        if (exportAll || tables.contains("storage_roots")) {
            List<StorageRoot> storage = storageRootRepository.findAll();
            rootNode.set("storage_roots", objectMapper.valueToTree(storage));
        }

        // vezmeme zformátovaný (DefaultPrettyPrinter provede formátování, nebude to jeden řádek ale bude to
        // odřádkované s mezeracmi) objekt JSONu v paměti a rovnou ho zapíšeme do síťového streamu; odpověď klientu
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(outputStream, rootNode);
    }

    /**
     * Načte JSON zálohu ze streamu a bezpečně ji v jedné transakci zapíše do DB.
     */
    @Transactional // Drží otevřenou session do DB po celou dobu, co Jackson generuje JSON.
    public void importDatabase(InputStream inputStream) throws IOException {
        // přečteme celý nahraný JSON strom; převede ho na objekt JsonNodů
        JsonNode rootNode = objectMapper.readTree(inputStream);

        // pořadí obnovy je důležité, folder potřebuje mít obnoveny uživatele (vlastníky) a soubory; sharePermission potřebuje vše

        // uživatelé
        if (rootNode.has("users")) {
            JsonNode usersNode = rootNode.get("users"); // vytáhne část JSONu pod klíčem users, která musí existovat ^
            List<User> usersToSave = new ArrayList<>();

            for (JsonNode userNode : usersNode) { // iterace přes uživatele v JSONu
                User user = objectMapper.treeToValue(userNode, User.class); // vezme data (JSON uzel) a převede je na instanci té třídy

                // pokud uživatel s tímto ID existuje, přeskočíme ho
                if (!userRepository.existsById(user.getId())) {
                    // usersToSave.add(user);
                    entityManager.createNativeQuery(
                                    "INSERT INTO users (id, username, password_hash, email, role) VALUES (?1, ?2, ?3, ?4, ?5)"
                            )
                            .setParameter(1, user.getId())
                            .setParameter(2, user.getUsername())
                            .setParameter(3, user.getPasswordHash())
                            .setParameter(4, user.getEmail())
                            .setParameter(5, user.getRole().name())
                            .executeUpdate();

                }
            }
            // userRepository.saveAll(usersToSave);
        }

        // disky
        if (rootNode.has("storage_roots")) {
            JsonNode storageRootsNode = rootNode.get("storage_roots");
            List<StorageRoot> storageRootsToSave = new ArrayList<>();

            for (JsonNode storageRootNode : storageRootsNode) {
                StorageRoot storage = objectMapper.treeToValue(storageRootNode, StorageRoot.class);
                if (!storageRootRepository.existsById(storage.getId())) {
                    entityManager.createNativeQuery(
                                    "INSERT INTO storage_roots (id, active, base_path, disk_name) VALUES (?1, ?2, ?3, ?4)"
                            )
                            .setParameter(1, storage.getId())
                            .setParameter(2, storage.isActive())
                            .setParameter(3, storage.getBasePath())
                            .setParameter(4, storage.getDiskName())
                            .executeUpdate();
                }
            }
            // storageRootRepository.saveAll(storageRootsToSave);
        }

        // složky
        if (rootNode.has("folders")) {
            JsonNode foldersNode = rootNode.get("folders");
            // List<Folder> foldersToSave = new ArrayList<>();

            for (JsonNode folderNode : foldersNode) {
                Folder folder = objectMapper.treeToValue(folderNode, Folder.class);

                if(folderNode.has("ownerId")) { // musí to být uvnitř toho JSONu
                    Long ownerId = folderNode.get("ownerId").asLong(); // najdeme, převedeme na Long
                    User user = userRepository.findById(ownerId).orElseThrow(() ->
                            new RuntimeException("Chyba importu: Vlastník složky s ID " + ownerId + " neexistuje."));
                    folder.setOwner(user); // ruční obnova cizího klíče do Hibernate
                }

                if (!folderRepository.existsById(folder.getId())) {
                    entityManager.createNativeQuery(
                                    "INSERT INTO folders (id, name, created_at, deleted, deleted_at, parent_id, owner_id) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)"
                            )
                            .setParameter(1, folder.getId())
                            .setParameter(2, folder.getName())
                            .setParameter(3, folder.getCreatedAt())
                            .setParameter(4, folder.isDeleted())
                            .setParameter(5, folder.getDeletedAt())
                            .setParameter(6, folder.getParent() != null ? folder.getParent().getId() : null)
                            .setParameter(7, folder.getOwner() != null ? folder.getOwner().getId() : null)
                            .executeUpdate();

                }
            }
            // zápis všech složek najednou (rychlejší)
            // folderRepository.saveAll(foldersToSave); // nejde použít Hibernate dělá update, protože Jackson načetl ID, která se musí ale zachovat kvůli vazbám
        }

        // soubory
        if (rootNode.has("stored_files")) {
            JsonNode filesNode = rootNode.get("stored_files");
            // List<StoredFile> filesToSave = new ArrayList<>();

            for (JsonNode fileNode : filesNode) {
                StoredFile file = objectMapper.treeToValue(fileNode, StoredFile.class);

                // vlastník souboru
                if (fileNode.has("ownerId")) {
                    Long ownerId = fileNode.get("ownerId").asLong();
                    User owner = userRepository.findById(ownerId)
                            .orElseThrow(() -> new RuntimeException("Chyba importu: Vlastník souboru s ID " + ownerId + " neexistuje."));
                    file.setOwner(owner);
                }

                // složka ve který ten soubor je
                if (fileNode.has("folderId") && !fileNode.get("folderId").isNull()) {
                    Long folderId = fileNode.get("folderId").asLong();
                    Folder folder = folderRepository.findById(folderId).orElse(null);
                    file.setFolder(folder); // Pokud složka existuje, přiřadíme, jinak zůstává null (root NASu)
                }

                if (!fileRepository.existsById(file.getId())) {
                    entityManager.createNativeQuery("""
                INSERT INTO stored_files (
                    id, content_type, created_at, deleted, deleted_at, 
                    edited_at, file_size, original_name, physical_path, folder_id, owner_id
                )
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)
                """)
                            .setParameter(1, file.getId())
                            .setParameter(2, file.getContentType())
                            .setParameter(3, file.getCreatedAt())
                            .setParameter(4, file.isDeleted())
                            .setParameter(5, file.getDeletedAt())
                            .setParameter(6, file.getEditedAt())
                            .setParameter(7, file.getFileSize())
                            .setParameter(8, file.getOriginalName())
                            .setParameter(9, file.getPhysicalPath())
                            .setParameter(10, file.getFolder() != null ? file.getFolder().getId() : null)
                            .setParameter(11, file.getOwner() != null ? file.getOwner().getId() : null)
                            .executeUpdate();
                }
            }
        }

        // sdílení
        if (rootNode.has("share_permissions")) {
            JsonNode sharePermissionsNode = rootNode.get("share_permissions");
            // List<SharePermission> sharePermissionsToSave = new ArrayList<>();

            for (JsonNode sharePermissionNode : sharePermissionsNode) {
                SharePermission permission = objectMapper.treeToValue(sharePermissionNode, SharePermission.class);

                // uživatel, kterému se sdílí
                if (sharePermissionNode.has("sharedWithUserId")) {
                    Long userId = sharePermissionNode.get("sharedWithUserId").asLong();
                    User sharedWith = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("Import selhal: Uživatel pro sdílení s ID " + userId + " neexistuje."));
                    permission.setSharedWith(sharedWith);
                }

                // Obnova cizího klíče složky
                if (sharePermissionNode.has("folderId") && !sharePermissionNode.get("folderId").isNull()) { // má klíč folderId; ale může být v JSONu přímo null, což by na asLong() padlo
                    Long folderId = sharePermissionNode.get("folderId").asLong();
                    Folder folder = folderRepository.findById(folderId).orElse(null); // najde adresář nebo pro neexistující adresár => uloží null (třeba někdo odstanil přímo z JSONu)
                    permission.setFolder(folder);
                }

                // Obnova cizího klíče souboru (převod textového UUID z JSONu na objekt UUID)
                if (sharePermissionNode.has("storedFileId") && !sharePermissionNode.get("storedFileId").isNull()) {
                    UUID fileId = UUID.fromString(sharePermissionNode.get("storedFileId").asText());
                    StoredFile storedFile = fileRepository.findById(fileId).orElse(null);
                    permission.setStoredFile(storedFile);
                }

                if (!sharePermissionRepository.existsById(permission.getId())) {
                    entityManager.createNativeQuery("""
                INSERT INTO share_permissions (id, can_write, created_at, folder_id, shared_with_user_id, stored_file_id)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                """)
                            .setParameter(1, permission.getId())
                            .setParameter(2, permission.isCanWrite())
                            .setParameter(3, permission.getCreatedAt())
                            .setParameter(4, permission.getFolder() != null ? permission.getFolder().getId() : null)
                            .setParameter(5, permission.getStoredFile() != null ? permission.getStoredFile().getId() : null)
                            .setParameter(6, permission.getStoredFile() != null ? permission.getStoredFile().getId() : null)
                            .executeUpdate();
                }
            }
        }
    }
}
