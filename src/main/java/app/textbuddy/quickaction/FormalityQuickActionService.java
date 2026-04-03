package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.FormalityLlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class FormalityQuickActionService {

    private static final String ERROR_MESSAGE = "Formality-Rewrite konnte nicht erstellt werden.";
    private static final String MISSING_OPTION_MESSAGE = "Formality-Option ist erforderlich.";
    private static final String INVALID_OPTION_MESSAGE = "Formality-Option ist ungueltig.";

    private final FormalityLlmClient formalityLlmClient;

    public FormalityQuickActionService(FormalityLlmClient formalityLlmClient) {
        this.formalityLlmClient = formalityLlmClient;
    }

    public void stream(FormalityQuickActionRequest request, QuickActionStreamHandler handler) {
        String original = normalize(request == null ? null : request.text());
        String language = normalize(request == null ? null : request.language());

        if (original.isBlank()) {
            handler.complete("");
            return;
        }

        Optional<FormalityPrompt> prompt = FormalityPrompt.fromOption(request == null ? null : request.option());

        if (prompt.isEmpty()) {
            handler.error(resolveOptionErrorMessage(request == null ? null : request.option()));
            return;
        }

        try {
            List<String> chunks = formalityLlmClient.streamFormality(original, language, prompt.get());
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

    private String resolveOptionErrorMessage(String option) {
        return normalize(option).isBlank() ? MISSING_OPTION_MESSAGE : INVALID_OPTION_MESSAGE;
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
