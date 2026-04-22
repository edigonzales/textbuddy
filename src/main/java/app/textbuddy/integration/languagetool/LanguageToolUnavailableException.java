package app.textbuddy.integration.languagetool;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class LanguageToolUnavailableException extends RuntimeException {

    public LanguageToolUnavailableException(String message) {
        super(message);
    }

    public LanguageToolUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
