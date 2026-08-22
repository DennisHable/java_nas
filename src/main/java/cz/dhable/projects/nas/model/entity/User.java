package cz.dhable.projects.nas.model.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity // anotace indikuje, že tato třída popisuje tabulku v db
@Table(name = "users") // explicitní název tabulky
public class User {
    @Id // primární klíč tabulky
    @GeneratedValue(strategy = GenerationType.IDENTITY) // automaticky generovaná hodnota (AUTO_INCREMENT)
    private Long id;

    @Column(nullable=false, unique=true) // pro každý atribut je vytvořen sloupec v tabulce;
    // tato anotace to dále upravuje; nepovolujeme prázdnou hodnotu ve sloupci a musí být unikátní
    private String username; // unikátní neprázdné uživatelské jméno

    @Column(nullable=false)
    private String passwordHash; // do db se uloží zahashované heslo se "solí"; pswdHash = hash(heslo + salt); BCrypt to spojí

    @Enumerated(EnumType.STRING) // enum jako String
    @Column(nullable=false)
    private Role role; // enum; určuje oprávnění uživatele

    @Column(nullable = false, unique = true)
    private String email;

    // kontruktory

    protected User() {
        // JPA vyžaduje bezparam. konstruktor pro entity; Hibernate nastaví atributy reflexí
        // očekává se public nebo protected konst.; protected je bezpečnější; pouze potomci a třídy ve stejným balíčku
    }

    public User(String username,
                String password,
                Role role,
                String email) {
        this.username = username;
        this.passwordHash = password;
        this.role = role;
        this.email = email;
    }

    // gettery
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }
}