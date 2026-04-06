package app.textbuddy.integration.languagetool;

import app.textbuddy.config.LanguageToolProperties;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class EmbeddedLanguageToolClient implements LanguageToolClient {

    private static final Set<String> NGRAM_LANGUAGE_CODES = Set.of("de", "en", "fr", "es");
    private static final Map<String, Set<String>> AUTO_LANGUAGE_HINTS = Map.of(
            "de-CH", Set.of(" der ", " die ", " das ", " und ", " ist ", " nicht ", " mit ", " für ", " grüezi "),
            "fr", Set.of(" le ", " la ", " les ", " un ", " une ", " est ", " avec ", " pour ", " bonjour "),
            "it", Set.of(" il ", " lo ", " gli ", " una ", " un ", " con ", " per ", " ciao ", " questo "),
            "en-US", Set.of(" the ", " and ", " is ", " are ", " with ", " this ", " that ", " hello ")
    );

    private final Optional<Path> ngramPath;

    public EmbeddedLanguageToolClient(LanguageToolProperties properties) {
        this.ngramPath = Objects.requireNonNull(properties).normalizedNgramPath();
    }

    @Override
    public List<LanguageToolMatch> check(String text, String language) {
        String normalizedText = normalize(text);

        if (normalizedText.isBlank()) {
            return List.of();
        }

        Language resolvedLanguage = resolveLanguage(normalize(language), normalizedText);
        JLanguageTool languageTool = resolvedLanguage.createDefaultJLanguageTool();
        activateLanguageModelRules(languageTool, resolvedLanguage);

        try {
            return languageTool.check(normalizedText).stream()
                    .map(this::mapMatch)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Eingebettetes LanguageTool konnte den Text nicht prüfen.", exception);
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
                match.getSuggestedReplacements().stream()
                        .map(this::normalize)
                        .filter(value -> !value.isBlank())
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                                List::copyOf
                        ))
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

        if (!normalized.isBlank()) {
            resolvedLanguage = Languages.getLanguageForShortCode(normalized);
        }

        if (resolvedLanguage == null && !normalized.isBlank()) {
            resolvedLanguage = Languages.getLanguageForLocale(Locale.forLanguageTag(normalized));
        }

        if (resolvedLanguage == null) {
            throw new IllegalArgumentException("Nicht unterstützte LanguageTool-Sprache: " + normalized);
        }

        return resolvedLanguage.getDefaultLanguageVariant();
    }

    private void activateLanguageModelRules(JLanguageTool languageTool, Language language) {
        if (ngramPath.isEmpty() || !NGRAM_LANGUAGE_CODES.contains(language.getShortCode())) {
            return;
        }

        Path configuredPath = ngramPath.get();

        if (!Files.isDirectory(configuredPath)) {
            throw new IllegalStateException(
                    "textbuddy.languagetool.ngram-path muss auf ein bestehendes Verzeichnis zeigen."
            );
        }

        try {
            languageTool.activateLanguageModelRules(configuredPath.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("LanguageTool-N-Gramm-Regeln konnten nicht aktiviert werden.", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
