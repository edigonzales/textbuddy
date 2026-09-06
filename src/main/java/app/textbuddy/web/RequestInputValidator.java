package app.textbuddy.web;

import app.textbuddy.config.TextbuddyProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Component
public final class RequestInputValidator {

    private final TextbuddyProperties.Input limits;

    public RequestInputValidator(TextbuddyProperties properties) {
        this.limits = properties.getInput();
    }

    public void text(String value) {
        requireMaxLength(value, limits.getMaxTextLength(), "Text ist zu lang.");
    }

    public void prompt(String value) {
        requireMaxLength(value, limits.getMaxPromptLength(), "Prompt ist zu lang.");
    }

    public void combinedText(String first, String second) {
        if (second == null) {
            return;
        }

        long combinedLength = (long) Objects.requireNonNullElse(first, "").length() + second.length();
        if (combinedLength > limits.getMaxTextLength()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Original und erster Entwurf sind zusammen zu lang. Maximal erlaubt: "
                            + limits.getMaxTextLength() + " Zeichen."
            );
        }
    }

    private void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message + " Maximal erlaubt: " + maxLength + " Zeichen.");
        }
    }
}
