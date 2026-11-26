package app.textbuddy.integration.languagetool;

import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public final class EmbeddedLanguageToolClient implements LanguageToolClient {

    private static final Map<String, Set<String>> AUTO_LANGUAGE_HINTS = Map.of(
            "de-CH", Set.of(" der ", " die ", " das ", " und ", " ist ", " nicht ", " mit ", " für ", " grüezi "),
            "fr", Set.of(" le ", " la ", " les ", " un ", " une ", " est ", " avec ", " pour ", " bonjour "),
            "it", Set.of(" il ", " lo ", " gli ", " una ", " un ", " con ", " per ", " ciao ", " questo "),
            "en-US", Set.of(" the ", " and ", " is ", " are ", " with ", " this ", " that ", " hello ")
    );

    @Override
    public List<LanguageToolMatch> check(String text, String language) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Language resolvedLanguage = resolveLanguage(normalize(language), text);
        JLanguageTool languageTool = resolvedLanguage.createDefaultJLanguageTool();

        try {
            return languageTool.check(text).stream()
                    .map(this::mapMatch)
                    .toList();
        } catch (IOException exception) {
            throw new LanguageToolUnavailableException("Eingebettetes LanguageTool konnte den Text nicht prüfen.", exception);
        }
    }

    private LanguageToolMatch mapMatch(RuleMatch match) {
        String ruleId = normalize(match.getSpecificRuleId());

        if (ruleId.isBlank() && match.getRule() != null) {
            ruleId = normalize(match.getRule().getId());
        }

        return new LanguageToolMatch(
                match.getFromPos(),
                Math.max(0, match.getToPos() - match.getFromPos()),
                normalize(match.getMessage()),
                normalize(match.getShortMessage()),
                ruleId,
                match.getSuggestedReplacements()
        );
    }

    private Language resolveLanguage(String requestedLanguage, String text) {
        if (requestedLanguage.isBlank() || requestedLanguage.equalsIgnoreCase("auto")) {
            return fallbackAutoLanguage(text);
        }

        return resolveConfiguredLanguage(requestedLanguage);
    }

    private Language fallbackAutoLanguage(String text) {
        String normalized = " " + text.toLowerCase(Locale.ROOT) + " ";
        Map<String, Integer> scores = new LinkedHashMap<>();

        AUTO_LANGUAGE_HINTS.forEach((languageCode, hints) -> {
            int score = hints.stream()
                    .mapToInt(hint -> normalized.contains(hint) ? 1 : 0)
                    .sum();

            scores.put(languageCode, score);
        });

        if (normalized.contains("ä") || normalized.contains("ö") || normalized.contains("ü")) {
            scores.computeIfPresent("de-CH", (key, value) -> value + 2);
        }

        if (normalized.contains("é") || normalized.contains("è") || normalized.contains("à")) {
            scores.computeIfPresent("fr", (key, value) -> value + 2);
        }

        if (normalized.contains("ì") || normalized.contains("ò") || normalized.contains("à")) {
            scores.computeIfPresent("it", (key, value) -> value + 2);
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .map(this::resolveConfiguredLanguage)
                .orElseGet(() -> resolveConfiguredLanguage("en-US"));
    }

    private Language resolveConfiguredLanguage(String languageCode) {
        String normalized = normalize(languageCode);
        Language resolvedLanguage = null;

        try {
            resolvedLanguage = Languages.getLanguageForShortCode(normalized);
            if (resolvedLanguage == null) {
                resolvedLanguage = Languages.getLanguageForLocale(Locale.forLanguageTag(normalized));
            }
        } catch (IllegalArgumentException exception) {
            throw new LanguageToolUnavailableException("Nicht unterstützte LanguageTool-Sprache.", exception);
        }

        if (resolvedLanguage == null) {
            throw new LanguageToolUnavailableException("Nicht unterstützte LanguageTool-Sprache.");
        }

        return resolvedLanguage.getDefaultLanguageVariant();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
