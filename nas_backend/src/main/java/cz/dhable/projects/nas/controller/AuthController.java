package cz.dhable.projects.nas.controller;

import cz.dhable.projects.nas.model.dto.UserInputReqDto;
import cz.dhable.projects.nas.model.dto.UserOutputReqDto;
import cz.dhable.projects.nas.model.entity.User;
import cz.dhable.projects.nas.service.AuthService;
import cz.dhable.projects.nas.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController // web. kontrolér; zpracovává příchozí HTTP požadavky; převádí návratové hodnoty do JSON/XML a posílá je klientovy
@RequestMapping("/api/auth") // mapování požadavků na třídu; určuje na které adrese má kontroler naslouchat - všechny metody v této třídě začínají na "/api/auth"
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login") // speciální případ RequestMapping; použitá HTTP metoda je POST; adresa jako argument
    public ResponseEntity<UserOutputReqDto> login(@RequestBody UserInputReqDto req, HttpServletRequest httpRequest) {
        // RequestBody - vezme data z těla příchozího HTTP požadavku (třeba JSON) a převede je na Java objekt
        return ResponseEntity.ok(authService.login(req, httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest httpRequest) {
        if (authService.logout(httpRequest)) {
            // logout proběhl úspěšně -> 200 OK
            return ResponseEntity.ok("Logout successful.");
        } else {
            // neexistující session -> 200 OK
            return ResponseEntity.ok("You have already logged out.");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<UserOutputReqDto> register(@RequestBody UserInputReqDto req) {
        // RequestBody zkonvertuje příchozí JSON na Java DTO.

        // 201 uživatel vytvořen v db; vracíme output DTO uživatele v těle odpovědi
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @GetMapping("/me")
    public ResponseEntity<UserOutputReqDto> me(Authentication auth) {
        // Spring Security automaticky vezme JSESSIONID cookie z požadavku,
        // vyhledá session v paměti serveru a pokud je validní, injektuje do auth naplněný objekt Authentication

        // nikdo není přihlášen (session neexistuje); vypršela platnost/ uživ. byl odhlášen => auth je null
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }

        // najdeme uživatele v db; auth.getName() vrací uživ. jméno, které se do kontextu uložilo při přihlášení
        User user = userService.findByUsername(auth.getName());

        // vrátíme bezpečné DTO čistě jen s veřejnými údaji o daném uživateli
        return ResponseEntity.ok(
                new UserOutputReqDto(user.getId(), user.getUsername(), user.getRole().name(), user.getEmail())
        );
    }

    @GetMapping("/init-status")
    public ResponseEntity<Boolean> getInitStatus() {
        // pokud je počet uživatelů v DB roven nule, vrátíme true (systém potřebuje první spuštění)
        return ResponseEntity.ok(userService.countUsers() == 0);
    }
}