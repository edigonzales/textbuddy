package app.textbuddy.textcorrection;

import app.textbuddy.integration.languagetool.LanguageToolClient;
import app.textbuddy.integration.languagetool.LanguageToolMatch;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class DefaultTextCorrectionService implements TextCorrectionService {

    private static final String DEFAULT_LANGUAGE = "auto";
    private static final Comparator<LanguageToolMatch> MATCH_ORDER =
            Comparator.comparingInt(LanguageToolMatch::offset)
                    .thenComparingInt(LanguageToolMatch::length);

    private final LanguageToolClient languageToolClient;

    public DefaultTextCorrectionService(LanguageToolClient languageToolClient) {
        this.languageToolClient = languageToolClient;
    }

    @Override
    public CorrectionResponse correct(CorrectionRequest request) {
        String original = request == null || request.text() == null ? "" : request.text();

        if (original.isBlank()) {
            return new CorrectionResponse(original, List.of());
        }

        String language = normalizeLanguage(request == null ? null : request.language());
        List<CorrectionBlock> blocks = languageToolClient.check(original, language).stream()
                .filter(match -> hasValidRange(match, original))
                .sorted(MATCH_ORDER)
                .map(this::toCorrectionBlock)
                .toList();

        return new CorrectionResponse(original, blocks);
    }

    private CorrectionBlock toCorrectionBlock(LanguageToolMatch match) {
        return new CorrectionBlock(
                match.offset(),
                match.length(),
                normalizeText(match.message()),
                normalizeText(match.shortMessage()),
                normalizeText(match.ruleId()),
                match.replacements().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        );
    }

    private boolean hasValidRange(LanguageToolMatch match, String original) {
        return match.offset() >= 0
                && match.length() > 0
                && match.offset() + match.length() <= original.length();
    }

    private String normalizeLanguage(String language) {
        String candidate = normalizeText(language);
        return candidate.isEmpty() ? DEFAULT_LANGUAGE : candidate;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
