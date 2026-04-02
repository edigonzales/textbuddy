package app.textbuddy.wordsynonym;

import app.textbuddy.integration.llm.WordSynonymLlmClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class DefaultWordSynonymService implements WordSynonymService {

    private static final int MAX_SYNONYMS = 5;

    private final WordSynonymLlmClient wordSynonymLlmClient;

    public DefaultWordSynonymService(WordSynonymLlmClient wordSynonymLlmClient) {
        this.wordSynonymLlmClient = wordSynonymLlmClient;
    }

    @Override
    public WordSynonymResponse synonyms(WordSynonymRequest request) {
        String word = normalize(request == null ? null : request.word());
        String context = normalize(request == null ? null : request.context());

        if (word.isBlank() || !isSingleWord(word) || context.isBlank()) {
            return new WordSynonymResponse(List.of());
        }

        Map<String, String> normalizedCandidates = new LinkedHashMap<>();

        for (String candidate : wordSynonymLlmClient.suggestSynonyms(word, context)) {
            String normalizedCandidate = normalize(candidate);

            if (normalizedCandidate.isBlank() || normalizedCandidate.equalsIgnoreCase(word)) {
                continue;
            }

            normalizedCandidates.putIfAbsent(
                    normalizedCandidate.toLowerCase(Locale.ROOT),
                    normalizedCandidate
            );

            if (normalizedCandidates.size() >= MAX_SYNONYMS) {
                break;
            }
        }

        return new WordSynonymResponse(List.copyOf(normalizedCandidates.values()));
    }

    private boolean isSingleWord(String word) {
        return word.codePoints().noneMatch(Character::isWhitespace);
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
