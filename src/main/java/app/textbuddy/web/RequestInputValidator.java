package app.textbuddy.web;

import app.textbuddy.config.TextbuddyProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public final class RequestInputValidator {

    private final TextbuddyProperties.Input limits;

    public RequestInputValidator(TextbuddyProperties properties) {
        this.limits = properties.getInput();
    }

    public void text(String value) {
        int maxLength = limits.getMaxTextLength();
        if (value != null && value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Text ist zu lang. Maximal erlaubt: " + maxLength + " Zeichen.");
        }
    }
}
