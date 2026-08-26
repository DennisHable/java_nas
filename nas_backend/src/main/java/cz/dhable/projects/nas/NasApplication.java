package cz.dhable.projects.nas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// vypneme auto generování výchozího uživatele a hesla
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
public class NasApplication {

	public static void main(String[] args) {
		SpringApplication.run(NasApplication.class, args);
	}

}
