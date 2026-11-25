package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorFixFinding;
import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.quickaction.SummarizePrompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "textbuddy.llm", name = "mode", havingValue = "stub")
public final class StubTextbuddyLlmClient implements TextbuddyLlmClient {

    @Override
    public String rewrite(String text, String language, SummarizePrompt summary) {
        String content = normalize(text);
        if (content.isBlank()) return "";
        return summary == null ? plainLanguage(content, language) : summarize(content, summary);
    }

    @Override
    public List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks) {
        String normalizedText = text == null ? "" : text;

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
                    replacementFor(rule.ruleId(), match.matchedText(), rule.suggestion())
            )));
        }

        return List.copyOf(matches);
    }

    @Override
    public List<String> fixAdvisor(List<AdvisorFixFinding> findings) {
        return findings.stream().map(AdvisorFixFinding::suggestion).toList();
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

    private static Optional<TextMatch> findFirstMatch(String text, AdvisorRuleCheck rule) {
        return rule.matchTerms().stream()
                .map(term -> findMatch(text, term))
                .flatMap(Optional::stream)
                .min(Comparator.comparingInt(TextMatch::startIndex));
    }

    private static Optional<TextMatch> findMatch(String text, String term) {
        String normalizedTerm = normalize(term);
        if (normalizedTerm.isBlank()) {
            return Optional.empty();
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerTerm = normalizedTerm.toLowerCase(Locale.ROOT);
        int index = lowerText.indexOf(lowerTerm);
        while (index >= 0 && !hasWordBoundaries(text, index, index + normalizedTerm.length(), normalizedTerm)) {
            index = lowerText.indexOf(lowerTerm, index + 1);
        }
        return index < 0 ? Optional.empty() : Optional.of(new TextMatch(
                text.substring(index, index + normalizedTerm.length()),
                index,
                index + normalizedTerm.length()
        ));
    }

    private static boolean hasWordBoundaries(String text, int start, int end, String term) {
        boolean left = !Character.isLetterOrDigit(term.charAt(0)) || start == 0
                || !Character.isLetterOrDigit(text.charAt(start - 1));
        boolean right = !Character.isLetterOrDigit(term.charAt(term.length() - 1)) || end == text.length()
                || !Character.isLetterOrDigit(text.charAt(end));
        return left && right;
    }

    private static String replacementFor(String ruleId, String matchedText, String fallback) {
        String term = matchedText.toLowerCase(Locale.ROOT);
        return switch (ruleId) {
            case "downloaden-statt-herunterladen" -> switch (term) {
                case "gedownloadet" -> "heruntergeladen";
                case "updaten" -> "aktualisieren";
                case "upgedatet" -> "aktualisiert";
                default -> "herunterladen";
            };
            case "meeting-und-feedback-pruefen" -> term.equals("feedback") ? "Rückmeldung" : "Besprechung";
            case "mitarbeiter-neutral-formulieren" -> "Mitarbeitende";
            case "buerger-neutral-formulieren" -> "Bevölkerung";
            case "alte-dass-schreibung" -> "dass";
            case "email-mit-bindestrich" -> "E-Mail";
            case "per-sofort-vermeiden" -> "ab sofort";
            case "zhd-ausschreiben" -> "zuhanden von";
            case "beiliegend-modernisieren" -> "Im Anhang";
            case "obgenannt-vermeiden" -> "genannt";
            default -> fallback;
        };
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
