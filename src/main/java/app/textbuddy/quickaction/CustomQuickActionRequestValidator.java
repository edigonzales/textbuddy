package app.textbuddy.quickaction;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CustomQuickActionRequestValidator {

    static final String MISSING_PROMPT_MESSAGE = "Custom-Prompt ist erforderlich.";
    static final String INVALID_PROMPT_MESSAGE = "Custom-Prompt ist ungültig.";

    public CustomQuickActionPrompt.ValidationResult validate(QuickActionStreamRequest request) {
        return CustomQuickActionPrompt.validate(request == null ? null : request.prompt());
    }

    public void validateOrThrow(QuickActionStreamRequest request) {
        CustomQuickActionPrompt.ValidationResult validationResult = validate(request);

        if (validationResult == CustomQuickActionPrompt.ValidationResult.MISSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MISSING_PROMPT_MESSAGE);
        }

        if (validationResult != CustomQuickActionPrompt.ValidationResult.VALID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_PROMPT_MESSAGE);
        }
    }

    public String resolveErrorMessage(CustomQuickActionPrompt.ValidationResult validationResult) {
        return validationResult == CustomQuickActionPrompt.ValidationResult.MISSING
                ? MISSING_PROMPT_MESSAGE
                : INVALID_PROMPT_MESSAGE;
    }
}
