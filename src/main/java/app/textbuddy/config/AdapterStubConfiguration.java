package app.textbuddy.config;

import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import app.textbuddy.integration.llm.BulletPointsLlmClient;
import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.llm.LlmClientFacade;
import app.textbuddy.integration.llm.PlainLanguageLlmClient;
import app.textbuddy.integration.llm.WordSynonymLlmClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

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
    PlainLanguageLlmClient plainLanguageLlmClient() {
        return (text, language) -> {
            String normalized = normalize(text);

            if (normalized.isBlank()) {
                return List.of();
            }

            String rewritten = normalized
                    .replace("komplizierte", "einfache")
                    .replace("kompliziert", "einfach")
                    .replace("Sachverhalt", "Thema")
                    .replace("spezifisch", "klar")
                    .replace("relevant", "wichtig")
                    .replace("praezise", "klar");

            String prefix = normalize(language).toLowerCase(Locale.ROOT).startsWith("en")
                    ? "In plain language: "
                    : "Kurz und einfach: ";
            String combined = prefix + rewritten;

            return splitIntoChunks(combined, 20);
        };
    }

    @Bean
    BulletPointsLlmClient bulletPointsLlmClient() {
        return (text, language) -> {
            String normalized = normalize(text);

            if (normalized.isBlank()) {
                return List.of();
            }

            List<String> bulletItems = splitIntoBulletPointItems(normalized);
            String bulletPoints = bulletItems.stream()
                    .map(item -> "- " + item)
                    .collect(Collectors.joining("\n"));

            return splitIntoChunks(bulletPoints, 18);
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

    private static List<String> splitIntoBulletPointItems(String value) {
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int index = 0; index < value.length(); index += 1) {
            char character = value.charAt(index);

            if (character == '\n') {
                appendBulletPointItem(items, current);
                current.setLength(0);
                continue;
            }

            current.append(character);

            if (character == '.' || character == '!' || character == '?' || character == ';') {
                appendBulletPointItem(items, current);
                current.setLength(0);
            }
        }

        appendBulletPointItem(items, current);
        return items.isEmpty() ? List.of(value) : List.copyOf(items);
    }

    private static void appendBulletPointItem(List<String> items, StringBuilder candidate) {
        String normalized = normalize(candidate.toString());

        if (!normalized.isBlank()) {
            items.add(normalized);
        }
    }

    private static List<String> splitIntoChunks(String value, int maxChunkLength) {
        List<String> chunks = new ArrayList<>();
        int index = 0;

        while (index < value.length()) {
            int nextIndex = Math.min(value.length(), index + maxChunkLength);

            if (nextIndex < value.length()) {
                int splitIndex = value.lastIndexOf(' ', nextIndex - 1);

                if (splitIndex > index) {
                    nextIndex = splitIndex + 1;
                }
            }

            chunks.add(value.substring(index, nextIndex));
            index = nextIndex;
        }

        return List.copyOf(chunks);
    }
}
