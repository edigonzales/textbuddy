package app.textbuddy.sentencerewrite;

import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SentenceRewriteService {

    private static final int MAX_ALTERNATIVES = 3;

    private final TextbuddyLlmClient llmClient;

    public SentenceRewriteService(TextbuddyLlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public SentenceRewriteResponse rewrite(SentenceRewriteRequest request) {
        String original = normalize(request == null ? null : request.sentence());
        String context = normalize(request == null ? null : request.context());

        if (original.isBlank()) {
            return new SentenceRewriteResponse(original, List.of());
        }

        List<String> alternatives = llmClient.rewriteSentence(original, context).stream()
                .map(this::normalize)
                .filter(candidate -> !candidate.isBlank())
                .filter(candidate -> !candidate.equals(original))
                .filter(candidate -> candidate.length() <= 320)
                .filter(this::looksLikeSentence)
                .distinct()
                .limit(MAX_ALTERNATIVES)
                .toList();

        return new SentenceRewriteResponse(original, alternatives);
    }

    private boolean looksLikeSentence(String candidate) {
        return candidate.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count() >= 3;
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
