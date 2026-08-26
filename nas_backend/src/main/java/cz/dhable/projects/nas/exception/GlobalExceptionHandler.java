package cz.dhable.projects.nas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // Říká Springu, že tato třída globálně sleduje všechny Controllery
public class GlobalExceptionHandler {

    /**
     * pokud kdekoli v aplikaci (v Service nebo Controlleru) vybublá SecurityException,
     * tato metoda ji odchytí a bezpečně ji přeloží na HTTP 403 Forbidden.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException ex) {
        // Vrátíme čistý stav 403 a zprávu z výjimky, aplikace ani test nespadne
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    /**
     * Zachytí obecné chyby (např. RuntimeException při nenalezení složky)
     * a vrátí HTTP 400 Bad Request.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
