package cz.dhable.projects.nas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableMethodSecurity
@Configuration // třída obsahuje definice bean (metody vytvářejících objekty) a konfig. aplikací
@EnableWebSecurity // aktivuje vlastní webové zabezpečení Springu; vypne výchozí nastavení,
// které chrání celou aplikaci, a umožní plně převzít kontrolu nad tím, kdo má přístup k jakým částem webu.
public class SecurityConfig {

    /*
    // pro login; AuthService ale nepoužívá
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }*/

    /**
     * bezpečnostní pravidla pro celou appku
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {

        http
                // zabránit cizím webům (jiná doména) číst naše data
                .cors(Customizer.withDefaults()) // aktivace CORS; propoj s konfigem dole v metodě
                // navíc je CSRF ochrana delegována na prohlížeč pomocí atributu SameSite a jeho hodnoty (Strict nebo Lax)
                // nastaveného na session cookie (JSESSIONID); cizí weby cookie nedokážou přibalit (resp. prohlížeč cookie k požadavku nepřidá = anonymní požadavek - bez cookie)
                // zabraňuje to operaci nějakého cizího webu, který by chtěl poslat požadavek na backend; backend by ho bez SameSite (Strict/Lax)
                // zpracoval, provedl, ale dle hlaviček cors by prohlížeč nedovolil číst cizímu webu navrácená data, ale oprace na backendu by se provedla!!
                .csrf(AbstractHttpConfigurer::disable)

                // Spring nebude vytvářet session cookies pro neanonymní/neautentizované dotazy
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )


                // říkáme Spring Security, aby identitu uživatele ukládal a zpětně načítal
                // ze standardní HTTP Session spravované Tomcatem; bez tohoto řádku by si Spring v bezestavovém
                // režimu nepamatoval přihlášeného uživatele mezi jednotlivými kliknutími
                // (resp. Spring Security by cookie ignoroval = nepodíval by se do té session spravované Tomcatem, uživ. by byl anonymní)
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                )

                // autorizace koncový bodů - kdo a kam smí přistupovat
                .authorizeHttpRequests(auth -> auth
                        // pro získání informací o aktuálním uživateli vyžadujeme platné přihlášení (authenticated)
                        // specifikujeme pouze metodu GET; pokud by se někdo pokusil o POST na /me, filtr ho nepustí
                        .requestMatchers(HttpMethod.GET,"/api/auth/me").authenticated()

                        // přidáváme ochranu pro odhlášení; odhlásit se může pouze ten, kdo je už přihlášen
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                        // veřejné endpointy
                        // jelikož zde není specifikovaná HTTP metoda (GET/POST/...),
                        // povolení platí pro všechny metody na těchto adresách
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/init-status").permitAll()

                        // vše ostatní, nespecifikováno výše – vyžaduje přihlášeného uživatele
                        .anyRequest().authenticated()

                        // role ADMIN jsou vyžadovány u příslušných metod pomocí anotace @PreAuthorize
                )

                // kompletně vypínáme vestavěné přihlašovací formuláře (HTML stránky generované Springem)
                // a standardní vyskakovací šedá okna prohlížeče (HTTP Basic);
                // přihlašovací dialog si vykresluje frontend sám
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // embed (pdf náhledy); Spring zakazuje zobrazování endpointů uvnitř prvků <iframe>,<embed>,<object>
                // ochrana proti Clickjackingu (podvržení neviditelné vrstvy nad web) je přepnuta
                // do režimu sameOrigin - frontend i backend stejná lokální identita (Same-Origin)
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                );

                // možnost vypnutí anonymního uživatele; curl bez cookie => 403 Forbidden;
                // požADAVKU přidělena indentita anonymousUser s rolí ROLE_ANONYMOUS; pravidlo výše to zařízne
                // uživatel má identitu ale nemá oprávnění => přístup odepřen -> 403; pak by to bylo 401 v "/me"
                // .anonymous(AbstractHttpConfigurer::disable);

        return http.build(); // sestavíme a vrátíme hotový security filtr
    }
    /**
     * Slouží pro hashování hesel (BCrypt); uvnitř AuthenticationManageru
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * konfigurace CORS (Cross-Origin Resource Sharing).
     * určuje, za jakých podmínek smí cizí domény komunikovat s tímto API
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // povolujeme komunikaci výhradně z adresy vašeho lokálního frontendu
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173")); // adresa frontendu (5173 = Vite)
        // seznam HTTP metod, které frontend smí na backend posílat
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // povolené typy HTTP hlaviček -- vše (např. "Authorization", "Cache-Control", "Content-Type")
        config.setAllowedHeaders(List.of("*"));
        // povoluje prohlížeči přenášet přihlašovací údaje (JSESSIONID cookie)
        // bez tohoto by prohlížeč sice cookie měl, šlo by to na server (SameSite=Strict je splněno)
        // Tomcat na backend by přijal požadavek, cookie je platná; zpracuje data a pošle odpověd zpět;
        // v hlavičce je CORS info o tom, zda jsou povoleny credentials; pokud by nebyly tak to prohlížeč z
        // bezpečnostních důvodů zahodí a JS na frontendu se to nedostane
        config.setAllowCredentials(true);

        // aplikujeme tuto konfiguraci na úplně všechny URL adresy backendu (/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
