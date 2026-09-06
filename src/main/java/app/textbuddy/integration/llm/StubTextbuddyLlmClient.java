package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.quickaction.CharacterSpeechPrompt;
import app.textbuddy.quickaction.FormalityPrompt;
import app.textbuddy.quickaction.MediumCurrentUser;
import app.textbuddy.quickaction.MediumPrompt;
import app.textbuddy.quickaction.QuickActionRequest;
import app.textbuddy.quickaction.QuickActionType;
import app.textbuddy.quickaction.SocialMediaPrompt;
import app.textbuddy.quickaction.SummarizePrompt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class StubTextbuddyLlmClient implements TextbuddyLlmClient {

    @Override
    public String rewrite(QuickActionType action, QuickActionRequest request, MediumCurrentUser currentUser) {
        String text = normalize(request.text());

        if (text.isBlank()) {
            return "";
        }

        return switch (action) {
            case PLAIN_LANGUAGE -> plainLanguage(
                    request.previousText() == null ? text : normalize(request.previousText()),
                    request.language()
            );
            case BULLET_POINTS -> items(text).stream().map(item -> "- " + item).collect(Collectors.joining("\n"));
            case PROOFREAD -> text
                    .replace("Teh", "The").replace("teh", "the")
                    .replace("Recieve", "Receive").replace("recieve", "receive")
                    .replace("Wierd", "Weird").replace("wierd", "weird");
            case SUMMARIZE -> summarize(text, SummarizePrompt.fromOption(request.option()).orElseThrow());
            case FORMALITY -> formality(text, FormalityPrompt.fromOption(request.option()).orElseThrow());
            case SOCIAL_MEDIA -> socialMedia(text, SocialMediaPrompt.fromOption(request.option()).orElseThrow());
            case MEDIUM -> medium(
                    text,
                    MediumPrompt.fromOption(request.option()).orElseThrow(),
                    currentUser == null ? MediumCurrentUser.placeholder() : currentUser
            );
            case CHARACTER_SPEECH -> characterSpeech(
                    text,
                    CharacterSpeechPrompt.fromOption(request.option()).orElseThrow()
            );
            case CUSTOM -> "Custom Rewrite\n\nAuftrag: " + request.prompt() + "\n\nErgebnis:\n" + text;
        };
    }

    @Override
    public List<String> rewriteSentence(String sentence, String context) {
        String normalized = normalize(sentence);

        if (normalized.isBlank()) {
            return List.of();
        }

        String punctuation = trailingPunctuation(normalized);
        String stem = punctuation.isEmpty()
                ? normalized
                : normalized.substring(0, normalized.length() - punctuation.length()).trim();
        LinkedHashSet<String> alternatives = new LinkedHashSet<>();
        alternatives.add("Kurz gesagt: " + stem + punctuation);
        alternatives.add("Anders formuliert: " + stem + punctuation);
        alternatives.add("Praeziser gesagt: " + stem + punctuation);
        return List.copyOf(alternatives);
    }

    @Override
    public List<String> suggestSynonyms(String word, String context) {
        String normalizedWord = normalize(word);

        if (normalizedWord.isBlank() || normalize(context).isBlank()) {
            return List.of();
        }

        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("holprig", List.of("hakelig", "unrund", "stockend"));
        values.put("schnell", List.of("rasch", "flink", "zuegig"));
        values.put("gut", List.of("stark", "solide", "passend"));
        values.put("wichtig", List.of("zentral", "relevant", "entscheidend"));
        values.put("klar", List.of("deutlich", "praezise", "eindeutig"));
        return values.getOrDefault(
                normalizedWord.toLowerCase(Locale.ROOT),
                List.of(
                        "praeziseres " + normalizedWord,
                        "passenderes " + normalizedWord,
                        "konkreteres " + normalizedWord
                )
        );
    }

    @Override
    public List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks) {
        String normalizedText = normalize(text);

        if (normalizedText.isBlank() || ruleChecks == null || ruleChecks.isEmpty()) {
            return List.of();
        }

        List<AdvisorRuleMatch> matches = new ArrayList<>();

        for (AdvisorRuleCheck rule : ruleChecks) {
            findFirstMatch(normalizedText, rule).ifPresent(match -> matches.add(new AdvisorRuleMatch(
                    rule.documentName(),
                    rule.ruleId(),
                    match.matchedText(),
                    excerptAround(normalizedText, match.startIndex(), match.endIndex()),
                    rule.message() + " Gefunden: '" + match.matchedText() + "'.",
                    rule.suggestion()
            )));
        }

        return List.copyOf(matches);
    }

    private String plainLanguage(String text, String language) {
        String rewritten = text
                .replace("komplizierte", "einfache")
                .replace("kompliziert", "einfach")
                .replace("Sachverhalt", "Thema")
                .replace("spezifisch", "klar")
                .replace("relevant", "wichtig")
                .replace("praezise", "klar");
        String prefix = normalize(language).toLowerCase(Locale.ROOT).startsWith("en")
                ? "In plain language: "
                : "Kurz und einfach: ";
        return prefix + rewritten;
    }

    private String summarize(String text, SummarizePrompt prompt) {
        List<String> sentences = items(text);
        return switch (prompt) {
            case SENTENCE -> "Kurzfassung: " + first(sentences);
            case THREE_SENTENCE -> "Kurzfassung in drei Saetzen: " + String.join(" ", first(sentences, 3));
            case PARAGRAPH -> "Zusammenfassung: " + String.join(" ", first(sentences, 4));
            case PAGE -> "Zusammenfassung auf etwa einer Seite:\n\n" + String.join(" ", first(sentences, 6));
            case MANAGEMENT_SUMMARY -> "Management Summary\n"
                    + "- Kernpunkt: " + first(sentences) + "\n"
                    + "- Einordnung: " + item(sentences, 1, first(sentences)) + "\n"
                    + "- Empfehlung: " + item(sentences, sentences.size() - 1, first(sentences));
        };
    }

    private String formality(String text, FormalityPrompt prompt) {
        return switch (prompt) {
            case FORMAL -> "Formell ueberarbeitet: " + text
                    .replace("Hallo", "Guten Tag").replace("hi", "guten Tag")
                    .replace("schnell", "zeitnah").replace("brauchen", "benoetigen")
                    .replace("brauch", "benoetige").replace("Danke", "Vielen Dank");
            case INFORMAL -> "Lockerer formuliert: " + text
                    .replace("Guten Tag", "Hallo").replace("zeitnah", "schnell")
                    .replace("benoetigen", "brauchen").replace("benoetige", "brauch")
                    .replace("Vielen Dank", "Danke");
        };
    }

    private String socialMedia(String text, SocialMediaPrompt prompt) {
        List<String> sentences = items(text);
        String lead = first(sentences);
        String support = item(sentences, 1, lead);
        return switch (prompt) {
            case BLUESKY -> "Bluesky-Post: " + lead + " Fokus: " + support;
            case INSTAGRAM -> "Instagram-Caption: " + lead + " " + support + " #textbuddy #launch";
            case LINKEDIN -> "LinkedIn-Post: " + lead + "\n\nTakeaway: " + support;
        };
    }

    private String medium(String text, MediumPrompt prompt, MediumCurrentUser user) {
        List<String> sentences = items(text);
        String lead = first(sentences);
        String support = item(sentences, 1, lead);
        String closing = item(sentences, sentences.size() - 1, lead);
        return switch (prompt) {
            case EMAIL -> "Betreff: Projektupdate\n\nHallo [Anrede],\n\n" + lead + " " + support
                    + "\n\nFreundliche Gruesse\n" + user.fullName() + "\n" + user.email();
            case OFFICIAL_LETTER -> "Offizielles Schreiben\n\nSehr geehrte Damen und Herren,\n\n"
                    + lead + " " + support + "\n\nMit freundlichen Gruessen";
            case PRESENTATION -> "Praesentation\n- Titel: " + lead + "\n- Kernpunkt: " + support
                    + "\n- Naechster Schritt: " + closing;
            case REPORT -> "Bericht\n\nZusammenfassung: " + lead + "\nDetails: " + support
                    + "\nAbschluss: " + closing;
        };
    }

    private String characterSpeech(String text, CharacterSpeechPrompt prompt) {
        List<String> sentences = items(text);
        String lead = first(sentences);
        String support = item(sentences, 1, lead);
        return switch (prompt) {
            case DIRECT_SPEECH -> "Direkte Rede\n\n\"" + lead + "\", sagte die Figur.\n\""
                    + support + "\", antwortete die andere Figur.";
            case INDIRECT_SPEECH -> "Indirekte Rede\n\nDie Figur sagte, dass " + withoutPunctuation(lead)
                    + ".\nDanach erklaerte die andere Figur, dass " + withoutPunctuation(support) + ".";
        };
    }

    private static List<String> items(String value) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (char character : value.toCharArray()) {
            if (character == '\n') {
                append(result, current);
                continue;
            }
            current.append(character);
            if (character == '.' || character == '!' || character == '?' || character == ';') {
                append(result, current);
            }
        }

        append(result, current);
        return result.isEmpty() ? List.of(value) : List.copyOf(result);
    }

    private static void append(List<String> values, StringBuilder candidate) {
        String normalized = normalize(candidate.toString());
        candidate.setLength(0);
        if (!normalized.isBlank()) {
            values.add(normalized);
        }
    }

    private static String first(List<String> values) {
        return values.isEmpty() ? "" : values.getFirst();
    }

    private static List<String> first(List<String> values, int count) {
        return values.isEmpty() ? List.of() : List.copyOf(values.subList(0, Math.min(values.size(), count)));
    }

    private static String item(List<String> values, int index, String fallback) {
        return index < 0 || index >= values.size() ? fallback : values.get(index);
    }

    private static String trailingPunctuation(String value) {
        int index = value.length();
        while (index > 0 && ".!?".indexOf(value.charAt(index - 1)) >= 0) {
            index -= 1;
        }
        return value.substring(index);
    }

    private static String withoutPunctuation(String value) {
        return value.replaceAll("[.!?;]+$", "").trim();
    }

    private static Optional<TextMatch> findFirstMatch(String text, AdvisorRuleCheck rule) {
        return rule.matchTerms().stream()
                .map(term -> findMatch(text, term))
                .flatMap(Optional::stream)
                .min(Comparator.comparingInt(TextMatch::startIndex));
    }

    private static Optional<TextMatch> findMatch(String text, String term) {
        String normalizedTerm = normalize(term);
        int index = text.toLowerCase(Locale.ROOT).indexOf(normalizedTerm.toLowerCase(Locale.ROOT));
        return index < 0 ? Optional.empty() : Optional.of(new TextMatch(
                text.substring(index, index + normalizedTerm.length()),
                index,
                index + normalizedTerm.length()
        ));
    }

    private static String excerptAround(String text, int start, int end) {
        int from = Math.max(0, start - 32);
        int to = Math.min(text.length(), end + 32);
        return (from > 0 ? "..." : "") + text.substring(from, to).trim() + (to < text.length() ? "..." : "");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record TextMatch(String matchedText, int startIndex, int endIndex) {
    }
}
