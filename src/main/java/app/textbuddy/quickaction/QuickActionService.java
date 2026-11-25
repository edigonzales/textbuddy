package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.LlmProviderException;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public final class QuickActionService {

    private static final String REQUIRED_OPTION = "Option ist erforderlich.";
    private static final String INVALID_OPTION = "Option ist ungültig.";

    private final TextbuddyLlmClient llmClient;

    public QuickActionService(TextbuddyLlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public QuickActionResponse execute(
            QuickActionType action,
            QuickActionRequest request
    ) {
        QuickActionRequest normalized = normalize(request);

        if (normalized.text().isBlank()) {
            return new QuickActionResponse("");
        }

        validate(action, normalized);
        String rewritten = Objects.requireNonNullElse(llmClient.rewrite(action, normalized), "");
        if (rewritten.isBlank()) {
            throw new LlmProviderException("LLM-Provider lieferte keinen Antworttext.");
        }
        return new QuickActionResponse(rewritten);
    }

    private void validate(QuickActionType action, QuickActionRequest request) {
        if (action == QuickActionType.SUMMARIZE) {
            requireOption(request.option(), SummarizePrompt.fromOption(request.option()).isPresent());
        }
    }

    private void requireOption(String option, boolean valid) {
        if (normalize(option).isBlank()) {
            badRequest(REQUIRED_OPTION);
        }
        if (!valid) {
            badRequest(INVALID_OPTION);
        }
    }

    private QuickActionRequest normalize(QuickActionRequest request) {
        return new QuickActionRequest(
                Objects.requireNonNullElse(request == null ? null : request.text(), ""),
                normalize(request == null ? null : request.language()),
                normalize(request == null ? null : request.option())
        );
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }

    private void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
