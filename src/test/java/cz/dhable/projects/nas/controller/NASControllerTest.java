package cz.dhable.projects.nas.controller;


import com.jayway.jsonpath.JsonPath;
import cz.dhable.projects.nas.model.dto.UserInputReqDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.dhable.projects.nas.model.entity.StorageRoot;
import cz.dhable.projects.nas.repository.StorageRootRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// obsaháhlejší integrační testy, bez mocků Service a Repa; testuje se reálné nasazení

@Transactional // rollback po každém testu, přidání testů nelze už použít (neproběhly by zápisy do db; až na konci)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NASControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private cz.dhable.projects.nas.repository.UserRepository userRepository;

    @Autowired
    private cz.dhable.projects.nas.repository.FolderRepository folderRepository;

    @Autowired
    private cz.dhable.projects.nas.repository.StoredFileRepository fileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StorageRootRepository storageRootRepository;

    @BeforeEach
    void setUp() {
        // vyčistíme kompletně DB před každým testem
        fileRepository.deleteAll();
        folderRepository.deleteAll();
        userRepository.deleteAll();
        storageRootRepository.deleteAll();

        // vytvoříme dočasný testovací disk v Linuxové složce /tmp (soubory vygenerované během testů se uloží tam)
        StorageRoot testDisk = new StorageRoot("/tmp/nas_test_storage", "Testovací RAM Disk");
        testDisk.setActive(true); // nastavíme jako AKTIVNÍ

        storageRootRepository.save(testDisk);
    }


    @Test
    @DisplayName("Test NAS: Registrace -> Login -> Vytvoření složky -> Nahrání souboru -> Kontrola obsahu")
    void testNasLifecycle() throws Exception {

        // registrace, login uživ.; získání platné relace
        UserInputReqDto userDto = new UserInputReqDto("nas_test_user", "heslo123", "a@b.cz");

        // příprava a spuštění virutálního HTTP požadavku; arg je builder req
        mockMvc.perform(post("/api/auth/register") // statická metoda - vytvoří req builder pro POST na danou adresu
                        .contentType(MediaType.APPLICATION_JSON) // přidá do req hlavičku "Content-Type: application/json"
                        .content(objectMapper.writeValueAsString(userDto))) // vložení dat do těla req; Java object -> text. json
                .andExpect(status().isCreated()); // nad výsledkem co backend vrátil pustíme kontrolu; HTTP stav. kód je 201, jinak err

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk()) // HTTP status očekáváme 200
                .andReturn(); // ukončení řetězu kontrol a navrácení komplet objektu; ten v sobě má celý simulovat HTTP požadavek i odpověď

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(); // vytáhneme vygenerovanou session

        // vytvoření složky v rootu NASu
        mockMvc.perform(post("/api/nas/folders/create")
                        .session(session)
                        .param("name", "My_Docs"))
                .andExpect(status().isCreated());

        // ověření, že složka je v rootu NASu
        MvcResult rootContentResult = mockMvc.perform(get("/api/nas/content")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subFolders[0].name").value("My_Docs"))
                .andReturn();

        String jsonResponse = rootContentResult.getResponse().getContentAsString();

        // vyhledá v poli subFolders prvek s názvem 'My_Docs' a vytáhne jeho ID do listu
        List<Integer> folderIds = JsonPath.read(jsonResponse, "$.subFolders[?(@.name == 'My_Docs')].id");

        if (folderIds.isEmpty()) {
            org.junit.jupiter.api.Assertions.fail("The My_Docs folder was not found in the response!");
        }

        // H2 v paměti vrací čísla jako Integer => převod na Long
        Long createdFolderId = folderIds.getFirst().longValue();
        
        // nahrání souboru do této složky
        // vytvoříme soubor pro test (název parametru musí být "file", jako v NASController)
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Obsah testovacího souboru pro NAS".getBytes()
        );

        mockMvc.perform(multipart("/api/nas/files/upload") // místo POST, MULTIPART - přenos binárních souborů
                        .file(mockFile) // do req. se vloží simulovaný soubor; Spring v NASControlleru toto pole zachytí do parametru @RequestParam("file") ... file
                        .session(session) // pridání session z loginu; jako kdyby prohlížeč poslal cookie JSESSIONID a pak Spring načetl session
                        .param("folderId", String.valueOf(createdFolderId))) // text. form. param. mapování na @RequestParam(value = "folderId")
                .andExpect(status().isCreated());

        // kontrola obsahu dané složky
        mockMvc.perform(get("/api/nas/content")
                        .session(session)
                        .param("folderId", String.valueOf(createdFolderId)))
                .andExpect(status().isOk())
                // jsonPath - vyhledávácí engine nad JSONem, "$.files[0].originalName" = vem kořen JSONu, jdi do
                // 'files', vem 1. prvek a podívej se na atribut 'originalName'; oveření zda to odpovídá "test-document.txt"
                .andExpect(jsonPath("$.files[0].originalName").value("test-document.txt"));
    }



    @Test
    @DisplayName("Test NAS: Registrace -> Login -> Nahrání -> Stažení (Ověření textu uvnitř)")
    void testUploadAndDownloadContent() throws Exception {
        // opět registrace uživatele a login
        UserInputReqDto userDto = new UserInputReqDto("nas_test_user", "heslo123", "a@b.cz");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(userDto)));
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(userDto))).andReturn().getRequest().getSession();

        // nahrání souboru
        String originalContent = "Tajny obsah NAS souboru 123";
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, originalContent.getBytes());

        // vezmeme ID vygenerovaného souboru z odpovědi backendu
        MvcResult uploadResult = mockMvc.perform(multipart("/api/nas/files/upload")
                        .file(mockFile)
                        .session(session))
                .andExpect(status().isCreated())
                .andReturn();

        String stringId = uploadResult.getResponse().getContentAsString();
        UUID fileId = UUID.fromString(stringId);

        // stažení souboru; kontrola jeho obsahu
        MvcResult downloadResult = mockMvc.perform(get("/api/nas/files/download/" + fileId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andReturn();

        // přečteme bajty z HTTP těla odpovědi a převedeme je na text
        String downloadedContent = downloadResult.getResponse().getContentAsString();

        // text stažený z NASu se musí přesně shodovat s nahráným textem
        org.junit.jupiter.api.Assertions.assertEquals(originalContent, downloadedContent);
    }

}

