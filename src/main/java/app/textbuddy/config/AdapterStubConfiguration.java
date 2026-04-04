package app.textbuddy.config;

import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import app.textbuddy.integration.llm.BulletPointsLlmClient;
import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.llm.FormalityLlmClient;
import app.textbuddy.integration.llm.LlmClientFacade;
import app.textbuddy.integration.llm.PlainLanguageLlmClient;
import app.textbuddy.integration.llm.ProofreadLlmClient;
import app.textbuddy.integration.llm.SocialMediaLlmClient;
import app.textbuddy.integration.llm.SummarizeLlmClient;
import app.textbuddy.integration.llm.WordSynonymLlmClient;
import app.textbuddy.quickaction.FormalityPrompt;
import app.textbuddy.quickaction.SocialMediaPrompt;
import app.textbuddy.quickaction.SummarizePrompt;
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
    ProofreadLlmClient proofreadLlmClient() {
        return (text, language) -> {
            String normalized = normalize(text);

            if (normalized.isBlank()) {
                return List.of();
            }

            String proofread = normalized
                    .replace("Teh", "The")
                    .replace("teh", "the")
                    .replace("Recieve", "Receive")
                    .replace("recieve", "receive")
                    .replace("Wierd", "Weird")
                    .replace("wierd", "weird");

            return splitIntoChunks(proofread, 20);
        };
    }

    @Bean
    FormalityLlmClient formalityLlmClient() {
        return (text, language, prompt) -> {
            String normalized = normalize(text);

            if (normalized.isBlank()) {
                return List.of();
            }

            String rewritten = switch (prompt) {
                case FORMAL -> "Formell ueberarbeitet: " + normalized
                        .replace("Hallo", "Guten Tag")
                        .replace("hi", "guten Tag")
                        .replace("schnell", "zeitnah")
                        .replace("brauchen", "benoetigen")
                        .replace("brauch", "benoetige")
                        .replace("Danke", "Vielen Dank");
                case INFORMAL -> "Lockerer formuliert: " + normalized
                        .replace("Guten Tag", "Hallo")
                        .replace("zeitnah", "schnell")
                        .replace("benoetigen", "brauchen")
                        .replace("benoetige", "brauch")
                        .replace("Vielen Dank", "Danke");
            };

            return splitIntoChunks(rewritten, 22);
        };
    }

    @Bean
    SocialMediaLlmClient socialMediaLlmClient() {
        return (text, language, prompt) -> {
            String normalized = normalize(text);

            if (normalized.isBlank()) {
                return List.of();
            }

            List<String> sentences = splitIntoBulletPointItems(normalized);
            String lead = firstItem(sentences);
            String support = itemAt(sentences, 1, firstItem(sentences));

            String rewritten = switch (prompt) {
                case BLUESKY -> "Bluesky-Post: " + lead + " Fokus: " + support;
                case INSTAGRAM -> "Instagram-Caption: " + lead + " " + support + " #textbuddy #launch";
                case LINKEDIN -> "LinkedIn-Post: " + lead + "\n\nTakeaway: " + support;
            };

            return splitIntoChunks(rewritten, 22);
        };
    }

    @Bean
    SummarizeLlmClient summarizeLlmClient() {
        return (text, language, prompt) -> {
            String normalized = normalize(text);

            if (normalized.isBlank()) {
                return List.of();
            }

            List<String> sentences = splitIntoBulletPointItems(normalized);
            String summary = switch (prompt) {
                case SENTENCE -> "Kurzfassung: " + firstItem(sentences);
                case THREE_SENTENCE -> "Kurzfassung in drei Saetzen: "
                        + String.join(" ", firstItems(sentences, 3));
                case PARAGRAPH -> "Zusammenfassung: "
                        + String.join(" ", firstItems(sentences, 4));
                case PAGE -> """
                        Zusammenfassung auf etwa einer Seite:

                        %s
                        """.formatted(String.join(" ", firstItems(sentences, 6)));
                case MANAGEMENT_SUMMARY -> """
                        Management Summary
                        - Kernpunkt: %s
                        - Einordnung: %s
                        - Empfehlung: %s
                        """.formatted(
                        firstItem(sentences),
                        itemAt(sentences, 1, firstItem(sentences)),
                        itemAt(sentences, sentences.size() - 1, firstItem(sentences))
                );
            };

            return splitIntoChunks(summary, 24);
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

    private static String firstItem(List<String> items) {
        return items.isEmpty() ? "" : items.getFirst();
    }

    private static List<String> firstItems(List<String> items, int count) {
        if (items.isEmpty()) {
            return List.of();
        }

        return List.copyOf(items.subList(0, Math.min(items.size(), count)));
    }

    private static String itemAt(List<String> items, int index, String fallback) {
        if (items.isEmpty() || index < 0 || index >= items.size()) {
            return fallback;
        }

        return items.get(index);
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
