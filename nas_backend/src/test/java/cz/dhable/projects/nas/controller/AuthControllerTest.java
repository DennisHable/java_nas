package cz.dhable.projects.nas.controller;

import cz.dhable.projects.nas.model.dto.UserInputReqDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // aktivuje paměťovou H2 databázi z application-test.properties
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper(); // převádí Java DTO objekty na JSON

    @Test
    @DisplayName("Kompletní scenář: Registrace -> Login -> Získání /me -> Logout -> Neautorizované /me")
    void fullAuthenticationLifecycleTest() throws Exception {

        // ==========================================
        // Registrace nového uživatele
        // ==========================================
        UserInputReqDto registerDto = new UserInputReqDto("test_user", "heslo123", "a@b.cz", false);

        // spustění simulovaného HTTP požadavku; podobně jako reálný požadavek pomocí curl
        mockMvc.perform(post("/api/auth/register")
                        // nastaví hlavičku na Content-Type: application/json; říká to backendu, že posíláme JSON data
                        .contentType(MediaType.APPLICATION_JSON)
                        // převod (serializace) instance třídy UserInputReqDto na JSON; vloží do těla požadavku
                        .content(objectMapper.writeValueAsString(registerDto)))

                // assertion (co očekáváme)

                .andExpect(status().isCreated()) // očekáváme návratový HTTP kód 201 -- Created; jiný kód -> selhání
                // ověření dat v JSONu
                // jsonPath -- nástroj pro procházení struktury JSON odpovědi; '$' reprezentuje kořen JSONu
                // cesta "$.userName" -> hodnota klíče username; .value(...) ověří, zda se text přesně shoduje s argumentem
                .andExpect(jsonPath("$.username").value("test_user"))
                // existuje pro něj id
                .andExpect(jsonPath("$.id").exists());

        // ==========================================
        // Neautorizovaný pokus o /me před přihlášením
        // ==========================================
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden()); // 403 Forbidden - anonym

        // ==========================================
        // 3. KROK: Přihlášení (Login) a získání Session
        // ==========================================
        UserInputReqDto loginDto = new UserInputReqDto("test_user", "heslo123", "a@b.cz", false);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk()) // očekáváme HTTP 200 OK
                .andExpect(jsonPath("$.username").value("test_user"))
                .andReturn();

        // vytáhneme z login req (vezme instanci MockHttpSession, kterou Tomcat na
        // pozadí vytvořil během úspěšného přihlášení) HTTP Session z paměti (obsahující uložené JSESSIONID;
        // v session je uložený Spring Security kontext)
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // ==========================================
        // úspěšné volání /me s přibalenou Session
        // ==========================================
        mockMvc.perform(get("/api/auth/me")
                        // přidání Session z loginu
                        // simulace situaace, kdy prohlížeč přibalil cookie JSESSIONID k odchozímu dotazu
                        // díky tomu server pozná, o jakého uživatele jde
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test_user"));

        // ==========================================
        // Logout
        // ==========================================
        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isOk());

        // ==========================================
        // Pokus o /me po odhlášení (Session byla zničena)
        // ==========================================
        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isForbidden());
    }
}