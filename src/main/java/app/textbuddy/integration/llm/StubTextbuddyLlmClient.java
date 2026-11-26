package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.advisor.AdvisorRuleMatch;
import app.textbuddy.quickaction.SummarizePrompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "textbuddy.llm", name = "mode", havingValue = "stub")
public final class StubTextbuddyLlmClient implements TextbuddyLlmClient {

    private static final Map<String, String> DEMO_REPLACEMENTS = Map.of(
            "downloaden", "herunterladen",
            "per sofort", "ab sofort",
            "z.hd.", "zuhanden von"
    );

    @Override
    public String rewrite(String text, String language, SummarizePrompt summary) {
        String content = normalize(text);
        if (content.isBlank()) return "";
        return summary == null ? plainLanguage(content, language) : summarize(content, summary);
    }

    @Override
    public List<AdvisorRuleMatch> validate(String text, List<AdvisorRuleCheck> ruleChecks) {
        List<AdvisorRuleMatch> matches = new ArrayList<>();
        for (AdvisorRuleCheck check : ruleChecks) {
            for (String term : check.rule().matchTerms()) {
                String replacement = DEMO_REPLACEMENTS.get(term.toLowerCase(Locale.ROOT));
                if (replacement == null) continue;
                var matcher = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(term) + "(?![\\p{L}\\p{N}])",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
                AdvisorRuleMatch finding = null;
                while (matcher.find()) {
                    String matchedText = matcher.group();
                    int previous = text.lastIndexOf(matchedText, matcher.start() - 1);
                    int next = text.indexOf(matchedText, matcher.end());
                    String excerpt = previous < 0 && next < 0 ? matchedText
                            : text.substring(previous + 1, next < 0 ? text.length() : next);
                    boolean unique = text.indexOf(excerpt) == text.lastIndexOf(excerpt);
                    if (finding == null || unique) {
                        finding = new AdvisorRuleMatch(check.document().name(), check.rule().id(), matchedText,
                                excerpt, check.rule().message(), replacement);
                    }
                    if (unique) break;
                }
                if (finding != null) {
                    matches.add(finding);
                    break;
                }
            }
        }
        return matches;
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
