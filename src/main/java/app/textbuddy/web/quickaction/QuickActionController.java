package app.textbuddy.web.quickaction;

import app.textbuddy.integration.llm.LlmProviderException;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import app.textbuddy.quickaction.QuickActionRequest;
import app.textbuddy.quickaction.QuickActionResponse;
import app.textbuddy.quickaction.SummarizePrompt;
import app.textbuddy.web.RequestInputValidator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@RestController
@RequestMapping("/api/quick-actions")
public final class QuickActionController {

    private final TextbuddyLlmClient llm;
    private final RequestInputValidator inputValidator;

    public QuickActionController(TextbuddyLlmClient llm, RequestInputValidator inputValidator) {
        this.llm = llm;
        this.inputValidator = inputValidator;
    }

    @PostMapping("/plain-language")
    public QuickActionResponse plainLanguage(@RequestBody QuickActionRequest request) {
        return rewrite(request, false);
    }

    @PostMapping("/summarize")
    public QuickActionResponse summarize(@RequestBody QuickActionRequest request) {
        return rewrite(request, true);
    }

    private QuickActionResponse rewrite(QuickActionRequest request, boolean summarize) {
        String text = Objects.requireNonNullElse(request.text(), "");
        inputValidator.text(text);
        if (text.isBlank()) {
            return new QuickActionResponse("");
        }

        SummarizePrompt summary = null;
        if (summarize) {
            String option = Objects.requireNonNullElse(request.option(), "").trim();
            summary = SummarizePrompt.fromOption(option).orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, option.isEmpty() ? "Option ist erforderlich." : "Option ist ungültig."
            ));
        }

        String rewritten = llm.rewrite(text, Objects.requireNonNullElse(request.language(), "").trim(), summary);
        if (rewritten == null || rewritten.isBlank()) {
            throw new LlmProviderException("LLM-Provider lieferte keinen Antworttext.");
        }
        return new QuickActionResponse(rewritten);
    }
}
