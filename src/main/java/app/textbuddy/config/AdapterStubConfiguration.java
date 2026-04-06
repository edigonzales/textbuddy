package app.textbuddy.config;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.integration.llm.AdvisorValidationLlmClient;
import app.textbuddy.integration.llm.BulletPointsLlmClient;
import app.textbuddy.integration.llm.CharacterSpeechLlmClient;
import app.textbuddy.integration.llm.CustomLlmClient;
import app.textbuddy.integration.llm.FormalityLlmClient;
import app.textbuddy.integration.llm.LlmClientFacade;
import app.textbuddy.integration.llm.MediumLlmClient;
import app.textbuddy.integration.llm.PlainLanguageLlmClient;
import app.textbuddy.integration.llm.ProofreadLlmClient;
import app.textbuddy.integration.llm.SocialMediaLlmClient;
import app.textbuddy.integration.llm.SummarizeLlmClient;
import app.textbuddy.integration.llm.WordSynonymLlmClient;
import app.textbuddy.quickaction.CharacterSpeechPrompt;
import app.textbuddy.quickaction.CustomQuickActionPrompt;
import app.textbuddy.quickaction.FormalityPrompt;
import app.textbuddy.quickaction.MediumCurrentUser;
import app.textbuddy.quickaction.MediumPrompt;
import app.textbuddy.quickaction.SocialMediaPrompt;
import app.textbuddy.quickaction.SummarizePrompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "textbuddy.llm", name = "mode", havingValue = "stub")
public class AdapterStubConfiguration {

    @Bean
    LlmClientFacade llmClientFacade() {
        return (sentence, context) -> {
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
    MediumLlmClient mediumLlmClient() {
        return (text, language, prompt, currentUser) -> {
            String normalized = normalize(text);
            MediumCurrentUser resolvedCurrentUser = currentUser == null ? MediumCurrentUser.placeholder() : currentUser;

            if (normalized.isBlank()) {
                return List.of();
            }

            List<String> sentences = splitIntoBulletPointItems(normalized);
            String lead = firstItem(sentences);
            String support = itemAt(sentences, 1, firstItem(sentences));
            String closing = itemAt(sentences, sentences.size() - 1, firstItem(sentences));

            String rewritten = switch (prompt) {
                case EMAIL -> """
                        Betreff: Projektupdate

                        Hallo [Anrede],

                        %s %s

                        Freundliche Gruesse
                        %s
                        %s
                        """.formatted(lead, support, resolvedCurrentUser.fullName(), resolvedCurrentUser.email());
                case OFFICIAL_LETTER -> """
                        Offizielles Schreiben

                        Sehr geehrte Damen und Herren,

                        %s %s

                        Mit freundlichen Gruessen
                        """.formatted(lead, support);
                case PRESENTATION -> """
                        Praesentation
                        - Titel: %s
                        - Kernpunkt: %s
                        - Naechster Schritt: %s
                        """.formatted(lead, support, closing);
                case REPORT -> """
                        Bericht

                        Zusammenfassung: %s
                        Details: %s
                        Abschluss: %s
                        """.formatted(lead, support, closing);
            };

            return splitIntoChunks(rewritten.stripTrailing(), 24);
        };
    }

    @Bean
    CharacterSpeechLlmClient characterSpeechLlmClient() {
        return (text, language, prompt) -> {
            String normalized = normalize(text);

            if (normalized.isBlank()) {
                return List.of();
            }

            List<String> sentences = splitIntoBulletPointItems(normalized);
            String lead = firstItem(sentences);
            String support = itemAt(sentences, 1, firstItem(sentences));

            String rewritten = switch (prompt) {
                case DIRECT_SPEECH -> """
                        Direkte Rede

                        "%s", sagte die Figur.
                        "%s", antwortete die andere Figur.
                        """.formatted(lead, support);
                case INDIRECT_SPEECH -> """
                        Indirekte Rede

                        Die Figur sagte, dass %s.
                        Danach erklaerte die andere Figur, dass %s.
                        """.formatted(
                        stripTrailingSentencePunctuation(lead),
                        stripTrailingSentencePunctuation(support)
                );
            };

            return splitIntoChunks(rewritten.stripTrailing(), 24);
        };
    }

    @Bean
    CustomLlmClient customLlmClient() {
        return (text, language, prompt) -> {
            String normalized = normalize(text);

            if (normalized.isBlank()) {
                return List.of();
            }

            String rewritten = """
                    Custom Rewrite

                    Auftrag: %s

                    Ergebnis:
                    %s
                    """.formatted(prompt.userPrompt(), normalized);

            return splitIntoChunks(rewritten.stripTrailing(), 24);
        };
    }

    @Bean
    AdvisorValidationLlmClient advisorValidationLlmClient() {
        return (text, ruleChecks) -> {
            String normalizedText = normalize(text);

            if (normalizedText.isBlank() || ruleChecks == null || ruleChecks.isEmpty()) {
                return List.of();
            }

            List<AdvisorRuleMatch> matches = new ArrayList<>();

            for (AdvisorRuleCheck ruleCheck : ruleChecks) {
                findFirstMatch(normalizedText, ruleCheck).ifPresent(match -> matches.add(new AdvisorRuleMatch(
                        ruleCheck.documentName(),
                        ruleCheck.ruleId(),
                        match.matchedText(),
                        excerptAround(normalizedText, match.startIndex(), match.endIndex()),
                        ruleCheck.message() + " Gefunden: '" + match.matchedText() + "'.",
                        ruleCheck.suggestion()
                )));
            }

            return List.copyOf(matches);
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

    private static String stripTrailingSentencePunctuation(String value) {
        String normalized = normalize(value);
        int index = normalized.length();

        while (index > 0) {
            char character = normalized.charAt(index - 1);

            if (character != '.' && character != '!' && character != '?' && character != ';') {
                break;
            }

            index -= 1;
        }

        return normalized.substring(0, index).trim();
    }

    private static Optional<TextMatch> findFirstMatch(String text, AdvisorRuleCheck ruleCheck) {
        return ruleCheck.matchTerms().stream()
                .map(term -> findMatch(text, term))
                .flatMap(Optional::stream)
                .min(Comparator.comparingInt(TextMatch::startIndex));
    }

    private static Optional<TextMatch> findMatch(String text, String term) {
        String normalizedText = normalize(text);
        String normalizedTerm = normalize(term);

        if (normalizedText.isBlank() || normalizedTerm.isBlank()) {
            return Optional.empty();
        }

        String lowerText = normalizedText.toLowerCase(Locale.ROOT);
        String lowerTerm = normalizedTerm.toLowerCase(Locale.ROOT);
        int matchIndex = lowerText.indexOf(lowerTerm);

        if (matchIndex < 0) {
            return Optional.empty();
        }

        return Optional.of(new TextMatch(
                normalizedText.substring(matchIndex, matchIndex + normalizedTerm.length()),
                matchIndex,
                matchIndex + normalizedTerm.length()
        ));
    }

    private static String excerptAround(String text, int startIndex, int endIndex) {
        int excerptStart = Math.max(0, startIndex - 32);
        int excerptEnd = Math.min(text.length(), endIndex + 32);
        String excerpt = text.substring(excerptStart, excerptEnd).trim();

        if (excerptStart > 0) {
            excerpt = "..." + excerpt;
        }
        if (excerptEnd < text.length()) {
            excerpt = excerpt + "...";
        }

        return excerpt;
    }

    private record TextMatch(
            String matchedText,
            int startIndex,
            int endIndex
    ) {
    }
}
