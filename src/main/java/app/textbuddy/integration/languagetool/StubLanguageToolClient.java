package app.textbuddy.integration.languagetool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StubLanguageToolClient implements LanguageToolClient {

    private static final Comparator<LanguageToolMatch> MATCH_ORDER =
            Comparator.comparingInt(LanguageToolMatch::offset)
                    .thenComparingInt(LanguageToolMatch::length);

    private static final List<StubRule> RULES = List.of(
            new StubRule(
                    Pattern.compile("\\bteh\\b", Pattern.CASE_INSENSITIVE),
                    "Possible spelling mistake found.",
                    "Spelling",
                    "STUB_SPELLING_TEH",
                    List.of("the")
            ),
            new StubRule(
                    Pattern.compile("\\brecieve\\b", Pattern.CASE_INSENSITIVE),
                    "Possible spelling mistake found.",
                    "Spelling",
                    "STUB_SPELLING_RECIEVE",
                    List.of("receive")
            ),
            new StubRule(
                    Pattern.compile(" {2,}"),
                    "Unnecessary whitespace found.",
                    "Whitespace",
                    "STUB_WHITESPACE",
                    List.of(" ")
            )
    );

    @Override
    public List<LanguageToolMatch> check(String text, String language) {
        return RULES.stream()
                .flatMap(rule -> rule.findMatches(text).stream())
                .sorted(MATCH_ORDER)
                .toList();
    }

    private record StubRule(
            Pattern pattern,
            String message,
            String shortMessage,
            String ruleId,
            List<String> replacements
    ) {

        private List<LanguageToolMatch> findMatches(String text) {
            Matcher matcher = pattern.matcher(text);
            List<LanguageToolMatch> matches = new ArrayList<>();

            while (matcher.find()) {
                matches.add(new LanguageToolMatch(
                        matcher.start(),
                        matcher.end() - matcher.start(),
                        message,
                        shortMessage,
                        ruleId,
                        replacements
                ));
            }

            return matches;
        }
    }
}
