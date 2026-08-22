package cz.dhable.projects.nas.controller;

import cz.dhable.projects.nas.model.dto.ShareRequestDto;
import cz.dhable.projects.nas.model.dto.UserInputReqDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.dhable.projects.nas.model.entity.StorageRoot;
import cz.dhable.projects.nas.repository.StorageRootRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // rollback dat po skončení testu
class SharePermissionTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private cz.dhable.projects.nas.repository.UserRepository userRepository;

    @Autowired
    private cz.dhable.projects.nas.repository.FolderRepository folderRepository;

    @Autowired
    private cz.dhable.projects.nas.repository.StoredFileRepository fileRepository;

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
    @DisplayName("Kompletní test sdílení: Uživatel A nahraje -> Uživatel B nemůže -> Uživatel A nasdílí -> Uživatel B stáhne")
    void testResourceSharingLifecycle() throws Exception {

        // registrace a login uživatele A (Vlastník) a uživatele B (Návštěvník)
        UserInputReqDto userADto = new UserInputReqDto("owner_user", "password123", "a@b.cz");
        UserInputReqDto userBDto = new UserInputReqDto("visitor_user", "password123", "c@b.cz");

        // registrace obou
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(userADto))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(userBDto))).andExpect(status().isCreated());

        // login obou pro získání dvou různých nezávislých Sessions
        MockHttpSession sessionA = (MockHttpSession) mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(userADto))).andReturn().getRequest().getSession();
        MockHttpSession sessionB = (MockHttpSession) mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(userBDto))).andReturn().getRequest().getSession();

        // uživatel A nahraje soubor na NAS
        String secretData = "Tento text uvidí jen ti, co mají práva!";
        MockMultipartFile mockFile = new MockMultipartFile("file", "tajny-plan.txt", MediaType.TEXT_PLAIN_VALUE, secretData.getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/nas/files/upload")
                        .file(mockFile)
                        .session(sessionA)) // nahrává Uživatel A
                .andExpect(status().isCreated())
                .andReturn();

        UUID sharedFileId = UUID.fromString(uploadResult.getResponse().getContentAsString());

        // uživatel B se pokusí soubor stáhnout bez oprávnění
        mockMvc.perform(get("/api/nas/files/download/" + sharedFileId)
                        .session(sessionB)) // zkouší stahovat uživatel B
                .andExpect(status().isForbidden()); // očekáváme 403 Forbidden (přístup odepřen)

        // uživatel A nasdílí soubor uživateli B (pouze pro čtení)
        ShareRequestDto shareDto = new ShareRequestDto(
                "visitor_user", // s kým sdílíme
                sharedFileId,   // ID souboru
                null,           // ID složky (sdílíme jen samotný soubor)
                false           // canWrite = false (pouze pro čtení)
        );

        mockMvc.perform(post("/api/nas/share")
                        .session(sessionA) // akci provádí vlastník (Uživatel A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareDto)))
                .andExpect(status().isOk());

        // uživatel B se znovu pokusí soubor stáhnout po nasdílení
        MvcResult downloadResult = mockMvc.perform(get("/api/nas/files/download/" + sharedFileId)
                        .session(sessionB)) // znovu stahuje Uživatel B
                .andExpect(status().isOk()) // nyní už musíme dostat 200 (OK)
                .andReturn();

        // ověříme, že Uživatel B reálně přečetl správná data ze souboru
        // přidáme argument StandardCharsets.UTF_8, aby Java správně přečetla české znaky
        String downloadedContent = downloadResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertEquals(secretData, downloadedContent);
    }
}
