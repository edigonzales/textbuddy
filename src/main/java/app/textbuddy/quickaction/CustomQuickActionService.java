package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.CustomLlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CustomQuickActionService {

    private static final String ERROR_MESSAGE = "Custom-Rewrite konnte nicht erstellt werden.";

    private final CustomLlmClient customLlmClient;
    private final CustomQuickActionRequestValidator requestValidator;

    public CustomQuickActionService(
            CustomLlmClient customLlmClient,
            CustomQuickActionRequestValidator requestValidator
    ) {
        this.customLlmClient = customLlmClient;
        this.requestValidator = requestValidator;
    }

    public void stream(QuickActionStreamRequest request, QuickActionStreamHandler handler) {
        String original = normalize(request == null ? null : request.text());
        String language = normalize(request == null ? null : request.language());

        if (original.isBlank()) {
            handler.complete("");
            return;
        }

        CustomQuickActionPrompt.ValidationResult validationResult = requestValidator.validate(request);

        if (validationResult != CustomQuickActionPrompt.ValidationResult.VALID) {
            handler.error(requestValidator.resolveErrorMessage(validationResult));
            return;
        }

        CustomQuickActionPrompt prompt = CustomQuickActionPrompt.fromInput(request == null ? null : request.prompt())
                .orElseThrow();

        try {
            List<String> chunks = customLlmClient.streamCustom(original, language, prompt);
            StringBuilder completeText = new StringBuilder();

            for (String chunk : chunks) {
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }

                handler.chunk(chunk);
                completeText.append(chunk);
            }

            if (completeText.isEmpty()) {
                completeText.append(original);
                handler.chunk(original);
            }

            handler.complete(completeText.toString());
        } catch (RuntimeException exception) {
            handler.error(ERROR_MESSAGE);
        }
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
