package app.textbuddy.config;

import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.llm.LlmClientFacade;
import app.textbuddy.integration.llm.WordSynonymLlmClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class AdapterStubConfiguration {

    @Bean
    LlmClientFacade llmClientFacade() {
        return sentence -> {
            String normalized = sentence == null ? "" : sentence.trim();

            if (normalized.isBlank()) {
                return List.of();
            }

            String punctuation = extractTrailingPunctuation(normalized);
            String stem = punctuation.isEmpty()
                    ? normalized
                    : normalized.substring(0, normalized.length() - punctuation.length()).trim();

            LinkedHashSet<String> alternatives = new LinkedHashSet<>();

            alternatives.add("Kurz gesagt: " + stem + punctuation);
            alternatives.add("Anders formuliert: " + stem + punctuation);
            alternatives.add("Praeziser gesagt: " + stem + punctuation);

            return List.copyOf(alternatives);
        };
    }

    @Bean
    WordSynonymLlmClient wordSynonymLlmClient() {
        return (word, context) -> {
            String normalizedWord = normalize(word);
            String normalizedContext = normalize(context);

            if (normalizedWord.isBlank() || normalizedContext.isBlank()) {
                return List.of();
            }

            String lowerWord = normalizedWord.toLowerCase(Locale.ROOT);
            Map<String, List<String>> synonymsByWord = new LinkedHashMap<>();

            synonymsByWord.put("holprig", List.of("hakelig", "unrund", "stockend"));
            synonymsByWord.put("schnell", List.of("rasch", "flink", "zuegig"));
            synonymsByWord.put("gut", List.of("stark", "solide", "passend"));
            synonymsByWord.put("wichtig", List.of("zentral", "relevant", "entscheidend"));
            synonymsByWord.put("klar", List.of("deutlich", "praezise", "eindeutig"));

            List<String> mapped = synonymsByWord.get(lowerWord);

            if (mapped != null) {
                return mapped;
            }

            return List.of(
                    "praeziseres " + normalizedWord,
                    "passenderes " + normalizedWord,
                    "konkreteres " + normalizedWord
            );
        };
    }

    @Bean
    DoclingClient doclingClient() {
        return new DoclingClient() {
        };
    }

    @Bean
    AdvisorDocumentRepository advisorDocumentRepository() {
        return new AdvisorDocumentRepository() {
        };
    }

    private static String extractTrailingPunctuation(String sentence) {
        int index = sentence.length();

        while (index > 0) {
            char current = sentence.charAt(index - 1);

            if (current != '.' && current != '!' && current != '?') {
                break;
            }

            index -= 1;
        }

        return sentence.substring(index);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
