package cz.dhable.projects.nas.service;

import cz.dhable.projects.nas.model.dto.UserInputReqDto;
import cz.dhable.projects.nas.model.dto.UserOutputReqDto;
import cz.dhable.projects.nas.model.entity.Role;
import cz.dhable.projects.nas.model.entity.User;
import cz.dhable.projects.nas.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service // business logic; vytvoří instanci a uloží ji do aplikačního kontextu; dependency injection v jiných třídách
public class AuthService {

    @Value("${app.security.remember-me-time:7d}")
    private Duration rememberMeTime; // Spring sám převede "7d" na objekt Duration; další (lepší) možností je nová konfig. třída (označená: @Configuration a @ConfigurationProperties(prefix = "app.security"))

    @Value("${app.security.standard-logout-time:30m}")
    private Duration standardLogoutTime;

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }



    // transakční zpracování; provedou se všechny operace nad db;
    // pokud skončí bez vyjímky, tak se transakce potvrdí (COMMIT); jinak se vše vše vrátí do
    // původního stavu před spuštěním té transakce (ROLLBACK)
    @Transactional
    public UserOutputReqDto register(UserInputReqDto request) {
        // pokud uživatel tohoto jména existuje, tak se vyhodí vyjímka
        if (userRepository.findByUsername(request.getUserName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        Role role = Role.USER;
        if(userService.countUsers() == 0) role = Role.ADMIN;

        User user = new User(
                request.getUserName(),
                passwordEncoder.encode(request.getPassword()),
                role,
                request.getEmail()
        );

        User saved = userRepository.save(user);

        return new UserOutputReqDto(saved.getId(), saved.getUsername(), saved.getRole().name(), saved.getEmail());
    }


    public UserOutputReqDto login(UserInputReqDto userInputReqDto, HttpServletRequest request) {
        // pokud uživatel tohoto jména neexistuje, tak se vyhodí vyjímka
        Optional<User> user = userRepository.findByUsername(userInputReqDto.getUserName());
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user name - no User found");
        }

        // kontrola hesla (porovnání hešů; heš hesla obsahuje i sůl, která se poté extrahuje a automaticky se přidá k
        // aktuálnímu heslu a to se pak zahešuje a porovná)
        if (!passwordEncoder.matches(userInputReqDto.getPassword(), user.get().getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        // vytvoříme pro uživatele identitu/„průkazku“ - info uložené v session na serveru
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.get().getUsername(),
                null, // heslo už je ověřené, v paměti ho nenecháváme
                List.of(new SimpleGrantedAuthority("ROLE_" + user.get().getRole().name()))
        );

        // získáme/vytvoříme(true) HTTP Session (Tomcat automaticky vygeneruje a pošle uživateli JSESSIONID cookie)
        HttpSession session = request.getSession(true);


        // nastavení expirace relace podle checkboxu
        if (Boolean.TRUE.equals(userInputReqDto.isRememberMe())) {
            session.setMaxInactiveInterval((int)rememberMeTime.getSeconds()); // jak dlouho bude Spring držet data o přihlášeném uživ. v session (v RAM) od poslední aktivity (tedy po aktivitě se to automaticky opět prodlouží)
        } else {
            session.setMaxInactiveInterval((int)standardLogoutTime.getSeconds());
        }


        // uložíme identitu uživatele do Security Contextu
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);

        // uzamkneme to do session
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);


        // vrátíme output DTO pro uživatele
        return new UserOutputReqDto(
                user.get().getId(),
                user.get().getUsername(),
                user.get().getRole().name(),
                user.get().getEmail());
    }

    public boolean logout(HttpServletRequest request) {
        // vezmeme aktuální session, pokud existuje (false = nevytvářej novou, pokud neexistuje)
        HttpSession session = request.getSession(false);

        // pokud existuje
        if (session != null) {
            session.invalidate(); // destrukce session na serveru (fyzické smazání z RAM/DB)

            // preventivně vyčistíme i Security Context aktuálního vlákna
            // Tomcat nemusí ničit vlákno a mohly by tam zůstat data jiného uživatele
            SecurityContextHolder.clearContext();
            return true;
        }

        return false; // session neexistovala
    }
}

