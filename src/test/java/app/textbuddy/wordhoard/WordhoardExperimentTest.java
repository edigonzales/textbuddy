package app.textbuddy.wordhoard;

import org.junit.jupiter.api.Test;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.language.SwissGerman;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolated experiment for a local lexical-difficulty signal based on wordhoard.
 * It deliberately lives in test code and is excluded from the normal test/check lifecycle.
 */
class WordhoardExperimentTest {

    private static final String RESOURCE_ROOT = "/wordhoard-experiment/";
    private static final String WORDHOARD_RELEASE = "v0.1.0";
    private static final String WORDHOARD_COMMIT = "1bc5730e8d6e682c416c03680b7cb8c6c7ca8cd0";
    private static final int EXPECTED_ROWS = 69_877;
    private static final int RARE_RANK = 20_000;
    private static final double MIN_PAIR_DIFFICULTY_DELTA = 0.02;
    private static final double ROBUST_MIN_IMPROVEMENT_RATE = 0.80;
    private static final double ROBUST_MAX_WORSENING_RATE = 0.10;
    private static final double ROBUST_MIN_KNOWN_COVERAGE = 0.80;
    private static final double LIMITED_MIN_IMPROVEMENT_RATE = 0.65;
    private static final double LIMITED_MIN_KNOWN_COVERAGE = 0.65;
    private static final double MIN_ZIX_DIRECTION_DELTA = 0.5;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void measuresLexicalDifficultyOnThePinnedCorpus() throws Exception {
        Manifest manifest = readJson("manifest.json", Manifest.class);
        byte[] compressedLexicon = readResource("lexicon.tsv.gz");
        validateManifest(manifest, compressedLexicon);

        Lexicon lexicon = Lexicon.read(compressedLexicon);
        validateLexicon(lexicon, manifest);
        List<ReferenceCase> referenceCases = readReferenceCases();
        validateCorpus(referenceCases);

        WordhoardPrototype prototype = new WordhoardPrototype(lexicon, new JLanguageTool(new SwissGerman()));
        List<CaseResult> caseResults = new ArrayList<>();
        for (ReferenceCase reference : referenceCases) {
            caseResults.add(new CaseResult(reference, prototype.measure(reference.text())));
        }

        assertThat(prototype.measure(referenceCases.getFirst().text()))
                .isEqualTo(caseResults.getFirst().measurement());
        caseResults.forEach(result -> validateMeasurement(result.measurement()));
        verifyDiagnostics(caseResults);

        List<PairResult> pairResults = evaluatePairs(caseResults);
        List<AdministrationTermResult> administrationTerms = inspectAdministrationTerms(lexicon, prototype);
        Summary summary = summarize(caseResults, pairResults);
        ExperimentReport report = new ExperimentReport(summary, pairResults, caseResults, administrationTerms);
        writeReports(manifest, lexicon, report);

        assertThat(caseResults.stream().filter(result -> result.reference().wordCount() >= 20)).hasSize(40);
        assertThat(caseResults.stream().filter(result -> result.reference().wordCount() < 20)).hasSize(12);
        assertThat(pairResults).hasSize(20);
        assertThat(administrationTerms).hasSize(10);
    }

    private static void validateManifest(Manifest manifest, byte[] compressedLexicon) {
        assertThat(manifest.schemaVersion()).isEqualTo(1);
        assertThat(manifest.source().release()).isEqualTo(WORDHOARD_RELEASE);
        assertThat(manifest.source().commit()).isEqualTo(WORDHOARD_COMMIT);
        assertThat(manifest.source().dataLicense()).isEqualTo("CC-BY-SA-4.0");
        assertThat(manifest.transformation().rowCount()).isEqualTo(EXPECTED_ROWS);
        assertThat(manifest.transformation().columns()).containsExactly(
                "lemma", "pos", "frequency_rank", "frequency_count", "cefr_estimate", "cefr_source"
        );
        assertThat(sha256(compressedLexicon)).isEqualTo(manifest.transformation().lexiconSha256());
        assertThat(manifest.transformation().cefrCounts()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "A1", 1_266,
                "A2", 1_412,
                "B1", 3_406,
                "B2", 4_022,
                "C1", 8_850,
                "C2", 50_921
        ));
    }

    private static void validateLexicon(Lexicon lexicon, Manifest manifest) {
        assertThat(lexicon.size()).isEqualTo(EXPECTED_ROWS);
        assertThat(lexicon.maxRank()).isEqualTo(EXPECTED_ROWS);
        assertThat(lexicon.cefrCounts()).containsExactlyInAnyOrderEntriesOf(manifest.transformation().cefrCounts());
        assertThat(lexicon.lookup("antrag", UniversalPos.NOUN)).get()
                .extracting(Lexeme::rank, Lexeme::cefr)
                .containsExactly(2_380, "A2");
        assertThat(lexicon.lookup("beantragen", UniversalPos.VERB)).get()
                .extracting(Lexeme::rank, Lexeme::cefr, Lexeme::cefrSource)
                .containsExactly(4_623, "B1", "anchor");
    }

    private static void validateCorpus(List<ReferenceCase> cases) {
        assertThat(cases).hasSize(52);
        assertThat(cases.stream().filter(item -> item.pairId() != null)).hasSize(40)
                .allSatisfy(item -> assertThat(item.wordCount()).isGreaterThanOrEqualTo(20));
        assertThat(cases.stream().filter(item -> item.pairId() == null)).hasSize(12);
        assertThat(cases.stream().filter(item -> item.pairId() != null)
                .map(ReferenceCase::pairId).distinct()).hasSize(20);
    }

    private static void validateMeasurement(LexicalMeasurement measurement) {
        assertThat(measurement.contentTokens()).isPositive();
        assertThat(measurement.knownTokens() + measurement.unknownTokens())
                .isEqualTo(measurement.contentTokens());
        assertThat(measurement.knownCoverage()).isBetween(0.0, 1.0);
        assertThat(measurement.unknownShare()).isBetween(0.0, 1.0);
        assertThat(measurement.rareShare()).isBetween(0.0, 1.0);
        assertThat(measurement.estimatedB1Coverage()).isBetween(0.0, 1.0);
        assertThat(measurement.estimatedB2Coverage()).isBetween(0.0, 1.0);
        assertThat(measurement.lexicalDifficulty()).isFinite();
        assertThat(measurement.knownCoverage() + measurement.unknownShare()).isCloseTo(1.0, within(1e-12));
    }

    private static void verifyDiagnostics(List<CaseResult> cases) {
        Map<String, CaseResult> byId = cases.stream()
                .collect(Collectors.toMap(result -> result.reference().id(), Function.identity()));
        assertThat(byId.keySet()).contains(
                "edge-swiss-ss", "edge-german-sharp-s", "edge-list-bullets",
                "edge-figures-money", "edge-name-unicode"
        );
        LexicalMeasurement names = byId.get("edge-name-unicode").measurement();
        assertThat(names.properNameTokens() + names.unknownTokens()).isPositive();
        assertThat(byId.get("edge-figures-money").measurement().excludedNumberTokens()).isPositive();
        assertThat(byId.get("edge-swiss-ss").measurement().contentTokens()).isPositive();
        assertThat(byId.get("edge-german-sharp-s").measurement().contentTokens()).isPositive();
    }

    private static List<PairResult> evaluatePairs(List<CaseResult> cases) {
        Map<String, List<CaseResult>> groups = cases.stream()
                .filter(result -> result.reference().pairId() != null)
                .collect(Collectors.groupingBy(result -> result.reference().pairId(), LinkedHashMap::new, Collectors.toList()));
        List<PairResult> pairs = new ArrayList<>();
        groups.forEach((pairId, values) -> {
            CaseResult original = values.stream()
                    .filter(value -> "original".equals(value.reference().role()))
                    .findFirst().orElseThrow();
            CaseResult simplified = values.stream()
                    .filter(value -> "simplified".equals(value.reference().role()))
                    .findFirst().orElseThrow();
            double difficultyDelta = original.measurement().lexicalDifficulty()
                    - simplified.measurement().lexicalDifficulty();
            Direction direction = Direction.fromDelta(difficultyDelta);
            double zixDelta = simplified.reference().zixScore() - original.reference().zixScore();
            boolean zixDecided = Math.abs(zixDelta) >= MIN_ZIX_DIRECTION_DELTA;
            boolean sameDirectionAsZix = !zixDecided
                    || Math.signum(zixDelta) == Math.signum(difficultyDelta);
            pairs.add(new PairResult(
                    pairId,
                    original.reference().category(),
                    difficultyDelta,
                    simplified.measurement().estimatedB1Coverage() - original.measurement().estimatedB1Coverage(),
                    simplified.measurement().estimatedB2Coverage() - original.measurement().estimatedB2Coverage(),
                    simplified.measurement().knownCoverage() - original.measurement().knownCoverage(),
                    zixDelta,
                    zixDecided,
                    sameDirectionAsZix,
                    direction
            ));
        });
        return pairs;
    }

    private static Summary summarize(List<CaseResult> cases, List<PairResult> pairs) {
        long improved = pairs.stream().filter(pair -> pair.direction() == Direction.IMPROVED).count();
        long worsened = pairs.stream().filter(pair -> pair.direction() == Direction.WORSENED).count();
        long tied = pairs.size() - improved - worsened;
        double improvementRate = ratio(improved, pairs.size());
        double worseningRate = ratio(worsened, pairs.size());
        List<CaseResult> eligible = cases.stream()
                .filter(result -> result.reference().wordCount() >= 20)
                .toList();
        double medianKnownCoverage = median(eligible.stream()
                .map(result -> result.measurement().knownCoverage()).toList());
        double medianUnknownShare = median(eligible.stream()
                .map(result -> result.measurement().unknownShare()).toList());
        long eligibleContentTokens = eligible.stream()
                .mapToLong(result -> result.measurement().contentTokens()).sum();
        long posFallbacks = eligible.stream()
                .mapToLong(result -> result.measurement().posFallbacks()).sum();
        long surfaceFallbacks = eligible.stream()
                .mapToLong(result -> result.measurement().surfaceFallbacks()).sum();
        List<PairResult> zixDecided = pairs.stream().filter(PairResult::zixDecided).toList();
        double zixDirectionAgreement = ratio(
                zixDecided.stream().filter(PairResult::sameDirectionAsZix).count(),
                zixDecided.size()
        );
        Verdict verdict;
        if (improvementRate >= ROBUST_MIN_IMPROVEMENT_RATE
                && worseningRate <= ROBUST_MAX_WORSENING_RATE
                && medianKnownCoverage >= ROBUST_MIN_KNOWN_COVERAGE) {
            verdict = Verdict.ROBUST_LEXICAL_SIGNAL;
        } else if (improvementRate >= LIMITED_MIN_IMPROVEMENT_RATE
                && medianKnownCoverage >= LIMITED_MIN_KNOWN_COVERAGE) {
            verdict = Verdict.LIMITED_DIAGNOSTIC_SIGNAL;
        } else {
            verdict = Verdict.NOT_SUFFICIENT;
        }
        return new Summary(
                verdict,
                eligible.size(),
                cases.size() - eligible.size(),
                pairs.size(),
                (int) improved,
                (int) worsened,
                (int) tied,
                improvementRate,
                worseningRate,
                medianKnownCoverage,
                medianUnknownShare,
                (int) eligibleContentTokens,
                ratio(posFallbacks, eligibleContentTokens),
                ratio(surfaceFallbacks, eligibleContentTokens),
                zixDecided.size(),
                zixDirectionAgreement
        );
    }

    private static List<AdministrationTermResult> inspectAdministrationTerms(
            Lexicon lexicon,
            WordhoardPrototype prototype
    ) throws IOException {
        List<AdministrationTermResult> results = new ArrayList<>();
        try (BufferedReader reader = resourceReader("swiss-administration-terms.tsv")) {
            String header = reader.readLine();
            assertThat(header).isEqualTo("term\tlemma\tpos");
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split("\t", -1);
                UniversalPos expectedPos = UniversalPos.valueOf(values[2]);
                Optional<Lexeme> direct = lexicon.lookup(normalize(values[1]), expectedPos);
                Optional<TokenAnalysis> languageTool = prototype.inspectSingleTerm(values[0]);
                results.add(new AdministrationTermResult(
                        values[0],
                        values[1],
                        expectedPos.name(),
                        direct.isPresent(),
                        direct.map(Lexeme::rank).orElse(null),
                        direct.map(Lexeme::cefr).orElse(null),
                        direct.map(Lexeme::cefrSource).orElse(null),
                        languageTool.map(TokenAnalysis::lemma).orElse(null),
                        languageTool.map(value -> value.pos().name()).orElse(null),
                        languageTool.flatMap(value -> lexicon.resolve(value.lemma(), value.pos(), value.surface()).lexeme()).isPresent()
                ));
            }
        }
        return results;
    }

    private static void writeReports(Manifest manifest, Lexicon lexicon, ExperimentReport report) throws IOException {
        Path reportDir = Path.of(System.getProperty(
                "wordhoard.reportDir",
                "build/reports/wordhoard-experiment"
        ));
        Files.createDirectories(reportDir);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("generatedAt", Instant.now().toString());
        output.put("source", manifest.source());
        output.put("languageToolVersion", "6.7");
        output.put("language", "SwissGerman");
        output.put("definitions", Map.of(
                "rareRankGreaterThan", RARE_RANK,
                "pairDifficultyDeadband", MIN_PAIR_DIFFICULTY_DELTA,
                "lexicalDifficulty", "mean log10 frequency rank; unknown content words use maxRank + 1",
                "estimatedCoverage", "share of content tokens at or below the wordhoard CEFR proxy level; unknowns stay in denominator"
        ));
        output.put("decisionThresholds", Map.of(
                "robustMinImprovementRate", ROBUST_MIN_IMPROVEMENT_RATE,
                "robustMaxWorseningRate", ROBUST_MAX_WORSENING_RATE,
                "robustMinMedianKnownCoverage", ROBUST_MIN_KNOWN_COVERAGE,
                "limitedMinImprovementRate", LIMITED_MIN_IMPROVEMENT_RATE,
                "limitedMinMedianKnownCoverage", LIMITED_MIN_KNOWN_COVERAGE
        ));
        output.put("lexicon", Map.of(
                "rows", lexicon.size(),
                "normalizedKeys", lexicon.uniqueKeyCount(),
                "normalizedKeyCollisions", lexicon.size() - lexicon.uniqueKeyCount(),
                "maximumRank", lexicon.maxRank(),
                "cefrCounts", lexicon.cefrCounts()
        ));
        output.put("summary", report.summary());
        output.put("pairs", report.pairs());
        output.put("administrationTerms", report.administrationTerms());
        output.put("cases", report.cases().stream().map(WordhoardExperimentTest::caseMap).toList());
        JSON.writerWithDefaultPrettyPrinter().writeValue(reportDir.resolve("report.json").toFile(), output);
        Files.writeString(reportDir.resolve("report.md"), markdown(manifest, report), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> caseMap(CaseResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("id", result.reference().id());
        output.put("pairId", result.reference().pairId());
        output.put("role", result.reference().role());
        output.put("category", result.reference().category());
        output.put("wordCount", result.reference().wordCount());
        output.put("eligible", result.reference().wordCount() >= 20);
        output.put("measurement", result.measurement());
        return output;
    }

    private static String markdown(Manifest manifest, ExperimentReport report) {
        Summary summary = report.summary();
        StringBuilder output = new StringBuilder("# wordhoard-/LanguageTool-Experiment\n\n");
        output.append("Daten: wordhoard ").append(manifest.source().release())
                .append(" (`").append(manifest.source().commit()).append("`, CC-BY-SA-4.0). ")
                .append("Analyse: LanguageTool 6.7 mit `SwissGerman`.\n\n")
                .append("## Urteil\n\n**").append(summary.verdict().label).append("**\n\n")
                .append(summary.verdict().explanation).append("\n\n")
                .append("| Messgrösse | Ergebnis | Robuste Grenze | Begrenzte Grenze |\n")
                .append("| --- | ---: | ---: | ---: |\n")
                .append(percentRow("Vereinfachungen lexikalisch leichter", summary.improvementRate(),
                        ROBUST_MIN_IMPROVEMENT_RATE, LIMITED_MIN_IMPROVEMENT_RATE))
                .append(percentRow("Vereinfachungen lexikalisch schwerer", summary.worseningRate(),
                        ROBUST_MAX_WORSENING_RATE, null))
                .append(percentRow("Median bekannte Inhaltswörter", summary.medianKnownCoverage(),
                        ROBUST_MIN_KNOWN_COVERAGE, LIMITED_MIN_KNOWN_COVERAGE))
                .append(percentRow("Median unbekannte Inhaltswörter", summary.medianUnknownShare(), null, null))
                .append(percentRow("Lemma-Treffer mit Wortart-Fallback", summary.posFallbackRate(), null, null))
                .append(percentRow("Treffer mit Oberflächen-Fallback", summary.surfaceFallbackRate(), null, null))
                .append(percentRow("Richtung wie offizieller ZIX", summary.zixDirectionAgreement(), null, null))
                .append("\nVon ").append(summary.pairCount()).append(" Paaren wurden ")
                .append(summary.improvedPairs()).append(" leichter, ")
                .append(summary.worsenedPairs()).append(" schwerer und ")
                .append(summary.tiedPairs()).append(" blieben innerhalb der Totzone von ±")
                .append(format(MIN_PAIR_DIFFICULTY_DELTA)).append(". ")
                .append(summary.eligibleCaseCount()).append(" Texte mit mindestens 20 Wörtern bilden die Auswertung; ")
                .append(summary.diagnosticCaseCount()).append(" kurze Fälle bleiben diagnostisch.\n\n")
                .append("Der Schwierigkeitswert ist der Mittelwert von `log10(Frequenzrang)`. ")
                .append("Unbekannte Inhaltswörter erhalten Rang ").append(EXPECTED_ROWS + 1)
                .append(". Ein kleinerer Wert bedeutet häufigeren Wortschatz. Das ist kein kalibrierter Gesamtscore.\n\n")
                .append("## Paare\n\n")
                .append("| Paar | Kategorie | Δ Schwierigkeit | Δ B1-Abdeckung | Δ B2-Abdeckung | Δ bekannt | Ergebnis |\n")
                .append("| --- | --- | ---: | ---: | ---: | ---: | --- |\n");
        for (PairResult pair : report.pairs()) {
            output.append("| ").append(pair.pairId()).append(" | ").append(pair.category()).append(" | ")
                    .append(signed(pair.difficultyDelta())).append(" | ")
                    .append(signedPercent(pair.b1CoverageDelta())).append(" | ")
                    .append(signedPercent(pair.b2CoverageDelta())).append(" | ")
                    .append(signedPercent(pair.knownCoverageDelta())).append(" | ")
                    .append(pair.direction().label).append(" |\n");
        }

        output.append("\n## Schweizer Verwaltungsbegriffe\n\n")
                .append("Direkter Datensatztreffer und tatsächliche LanguageTool-Auflösung werden getrennt gezeigt.\n\n")
                .append("| Begriff | Datensatz | Rang | CEFR-Proxy | Quelle | LT Lemma/POS | LT-Treffer |\n")
                .append("| --- | --- | ---: | --- | --- | --- | --- |\n");
        for (AdministrationTermResult term : report.administrationTerms()) {
            output.append("| ").append(term.term()).append(" | ")
                    .append(term.directlyKnown() ? "bekannt" : "unbekannt").append(" | ")
                    .append(term.rank() == null ? "–" : term.rank()).append(" | ")
                    .append(orDash(term.cefr())).append(" | ")
                    .append(orDash(term.cefrSource())).append(" | ")
                    .append(orDash(term.languageToolLemma())).append('/')
                    .append(orDash(term.languageToolPos())).append(" | ")
                    .append(term.languageToolLookupKnown() ? "bekannt" : "unbekannt").append(" |\n");
        }

        output.append("\n## Diagnostische Randfälle\n\n")
                .append("| Text | Kategorie | Inhalt | bekannt | unbekannt | Eigennamen | Zahlen | Schwierigkeit |\n")
                .append("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |\n");
        report.cases().stream()
                .filter(result -> result.reference().wordCount() < 20)
                .forEach(result -> output.append("| ").append(result.reference().id()).append(" | ")
                        .append(result.reference().category()).append(" | ")
                        .append(result.measurement().contentTokens()).append(" | ")
                        .append(result.measurement().knownTokens()).append(" | ")
                        .append(result.measurement().unknownTokens()).append(" | ")
                        .append(result.measurement().properNameTokens()).append(" | ")
                        .append(result.measurement().excludedNumberTokens()).append(" | ")
                        .append(format(result.measurement().lexicalDifficulty())).append(" |\n"));

        output.append("\n## Methodische Grenzen\n\n")
                .append("- `cefr_estimate` ist laut wordhoard ein frequenzbasierter Proxy, keine verlässliche CEFR-Zuordnung eines Wortes oder Textes.\n")
                .append("- Frequenzrang und Untertitelkorpus können Schweizer Verwaltungssprache, Fachbegriffe und Komposita systematisch benachteiligen.\n")
                .append("- Eigennamen werden nur ausgeschlossen, wenn LanguageTool sie als solche erkennt. Zahlen und Satzzeichen gehen nicht in die lexikalische Messung ein.\n")
                .append("- Die B1-/B2-Abdeckung ist eine erklärbare Unterdimension. Sie darf nicht als `Der Text ist B2` ausgegeben werden.\n")
                .append("- Lesbarkeit, Syntax, Kohärenz, fachliche Richtigkeit und semantische Verständlichkeit werden nicht gemessen.\n")
                .append("- Das Ergebnis rechtfertigt keine produktive Integration; dafür braucht es ein manuell bewertetes Schweizer Verwaltungskorpus.\n");
        return output.toString();
    }

    private static String percentRow(String label, double result, Double robust, Double limited) {
        return "| " + label + " | " + formatPercent(result) + " | "
                + (robust == null ? "–" : formatPercent(robust)) + " | "
                + (limited == null ? "–" : formatPercent(limited)) + " |\n";
    }

    private static List<ReferenceCase> readReferenceCases() throws IOException {
        try (InputStream input = requiredResource("/zix-compatibility/reference.json")) {
            JsonNode root = JSON.readTree(input);
            List<ReferenceCase> cases = new ArrayList<>();
            for (JsonNode item : root.get("cases")) {
                JsonNode pair = item.get("pairId");
                cases.add(new ReferenceCase(
                        item.get("id").stringValue(),
                        pair == null || pair.isNull() ? null : pair.stringValue(),
                        item.get("role").stringValue(),
                        item.get("category").stringValue(),
                        item.get("text").stringValue(),
                        item.get("wordCount").asInt(),
                        item.get("zixScore").asDouble()
                ));
            }
            return cases;
        }
    }

    private static <T> T readJson(String name, Class<T> type) throws IOException {
        try (InputStream input = requiredResource(RESOURCE_ROOT + name)) {
            return JSON.readValue(input, type);
        }
    }

    private static byte[] readResource(String name) throws IOException {
        try (InputStream input = requiredResource(RESOURCE_ROOT + name)) {
            return input.readAllBytes();
        }
    }

    private static BufferedReader resourceReader(String name) {
        return new BufferedReader(new InputStreamReader(
                requiredResource(RESOURCE_ROOT + name),
                StandardCharsets.UTF_8
        ));
    }

    private static InputStream requiredResource(String name) {
        InputStream input = WordhoardExperimentTest.class.getResourceAsStream(name);
        if (input == null) {
            throw new IllegalStateException("Missing test resource: " + name);
        }
        return input;
    }

    private static String sha256(byte[] data) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.GERMAN);
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

    private static double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        if (sorted.isEmpty()) {
            return Double.NaN;
        }
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 0
                ? (sorted.get(middle - 1) + sorted.get(middle)) / 2.0
                : sorted.get(middle);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f %%", value * 100.0);
    }

    private static String signed(double value) {
        return String.format(Locale.ROOT, "%+.4f", value);
    }

    private static String signedPercent(double value) {
        return String.format(Locale.ROOT, "%+.1f %%", value * 100.0);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "–" : value;
    }

    private static final class WordhoardPrototype {

        private static final Pattern WORD = Pattern.compile("\\p{L}+(?:[-’']\\p{L}+)*");
        private static final Pattern NUMBER = Pattern.compile("[+-]?(?:\\d+(?:[.,'’:/-]\\d+)*)%?");

        private final Lexicon lexicon;
        private final JLanguageTool languageTool;

        private WordhoardPrototype(Lexicon lexicon, JLanguageTool languageTool) {
            this.lexicon = lexicon;
            this.languageTool = languageTool;
        }

        private LexicalMeasurement measure(String text) throws IOException {
            int contentTokens = 0;
            int knownTokens = 0;
            int unknownTokens = 0;
            int rareTokens = 0;
            int b1Tokens = 0;
            int b2Tokens = 0;
            int properNameTokens = 0;
            int excludedNumberTokens = 0;
            int posFallbacks = 0;
            int surfaceFallbacks = 0;
            double logRankTotal = 0.0;
            List<Integer> knownRanks = new ArrayList<>();
            Map<String, MutableTermSignal> difficultTerms = new HashMap<>();

            for (AnalyzedSentence sentence : languageTool.analyzeText(text)) {
                for (AnalyzedTokenReadings readings : sentence.getTokensWithoutWhitespace()) {
                    String surface = readings.getToken();
                    if (readings.isSentenceStart() || surface == null || surface.isBlank()) {
                        continue;
                    }
                    if (NUMBER.matcher(surface).matches()) {
                        excludedNumberTokens++;
                        continue;
                    }
                    if (!WORD.matcher(surface).matches()) {
                        continue;
                    }

                    TokenClassification classification = classify(readings, surface);
                    if (classification.properName() && classification.analysis() == null) {
                        properNameTokens++;
                        continue;
                    }
                    if (classification.analysis() == null) {
                        if (classification.hasTaggedReading()) {
                            continue;
                        }
                        classification = new TokenClassification(
                                new TokenAnalysis(surface, normalize(surface), UniversalPos.NOUN),
                                false,
                                false
                        );
                    }

                    contentTokens++;
                    TokenAnalysis token = classification.analysis();
                    Resolution resolution = lexicon.resolve(token.lemma(), token.pos(), token.surface());
                    if (classification.properName() && resolution.lexeme().isEmpty()) {
                        contentTokens--;
                        properNameTokens++;
                        continue;
                    }
                    if (resolution.lexeme().isEmpty()) {
                        unknownTokens++;
                        logRankTotal += Math.log10(lexicon.maxRank() + 1.0);
                        difficultTerms.computeIfAbsent(token.lemma(), ignored -> new MutableTermSignal(token.lemma(), null))
                                .count++;
                        continue;
                    }

                    Lexeme lexeme = resolution.lexeme().get();
                    knownTokens++;
                    knownRanks.add(lexeme.rank());
                    logRankTotal += Math.log10(lexeme.rank());
                    if (resolution.kind() == ResolutionKind.POS_FALLBACK) {
                        posFallbacks++;
                    } else if (resolution.kind() == ResolutionKind.SURFACE_FALLBACK) {
                        surfaceFallbacks++;
                    }
                    if (lexeme.rank() > RARE_RANK) {
                        rareTokens++;
                        difficultTerms.computeIfAbsent(lexeme.lemma(), ignored -> new MutableTermSignal(lexeme.lemma(), lexeme))
                                .count++;
                    }
                    int cefr = cefrOrdinal(lexeme.cefr());
                    if (cefr <= cefrOrdinal("B1")) {
                        b1Tokens++;
                    }
                    if (cefr <= cefrOrdinal("B2")) {
                        b2Tokens++;
                    }
                }
            }
            if (contentTokens == 0) {
                throw new IllegalArgumentException("Text has no measurable content words");
            }
            knownRanks.sort(Integer::compareTo);
            List<TermSignal> topTerms = difficultTerms.values().stream()
                    .sorted(Comparator
                            .comparing((MutableTermSignal item) -> item.lexeme == null ? 0 : 1)
                            .thenComparing((MutableTermSignal item) -> item.count, Comparator.reverseOrder())
                            .thenComparing((MutableTermSignal item) -> item.lexeme == null ? Integer.MAX_VALUE : item.lexeme.rank(), Comparator.reverseOrder())
                            .thenComparing(item -> item.lemma))
                    .limit(8)
                    .map(MutableTermSignal::freeze)
                    .toList();
            return new LexicalMeasurement(
                    contentTokens,
                    knownTokens,
                    unknownTokens,
                    rareTokens,
                    properNameTokens,
                    excludedNumberTokens,
                    posFallbacks,
                    surfaceFallbacks,
                    ratio(knownTokens, contentTokens),
                    ratio(unknownTokens, contentTokens),
                    ratio(rareTokens, contentTokens),
                    ratio(b1Tokens, contentTokens),
                    ratio(b2Tokens, contentTokens),
                    logRankTotal / contentTokens,
                    knownRanks.isEmpty() ? null : knownRanks.get(knownRanks.size() / 2),
                    topTerms
            );
        }

        private Optional<TokenAnalysis> inspectSingleTerm(String term) throws IOException {
            for (AnalyzedSentence sentence : languageTool.analyzeText(term + ".")) {
                for (AnalyzedTokenReadings readings : sentence.getTokensWithoutWhitespace()) {
                    if (term.equals(readings.getToken())) {
                        TokenClassification result = classify(readings, term);
                        return Optional.ofNullable(result.analysis());
                    }
                }
            }
            return Optional.empty();
        }

        private static TokenClassification classify(AnalyzedTokenReadings readings, String surface) {
            boolean properName = false;
            boolean tagged = false;
            TokenAnalysis analysis = null;
            for (AnalyzedToken reading : readings) {
                String tag = reading.getPOSTag();
                if (tag == null || tag.isBlank()) {
                    continue;
                }
                tagged = true;
                UniversalPos pos = mapPos(tag);
                if (pos == UniversalPos.PROPN) {
                    properName = true;
                    continue;
                }
                if (pos != null && analysis == null) {
                    String lemma = reading.getLemma();
                    analysis = new TokenAnalysis(
                            surface,
                            normalize(lemma == null || lemma.isBlank() ? surface : lemma),
                            pos
                    );
                }
            }
            return new TokenClassification(analysis, properName, tagged);
        }

        private static UniversalPos mapPos(String tag) {
            if (tag.startsWith("SUB:")) {
                return UniversalPos.NOUN;
            }
            if (tag.startsWith("VER:")) {
                return UniversalPos.VERB;
            }
            if (tag.startsWith("ADJ:")) {
                return UniversalPos.ADJ;
            }
            if (tag.startsWith("ADV:")) {
                return UniversalPos.ADV;
            }
            if (tag.startsWith("EIG:")) {
                return UniversalPos.PROPN;
            }
            return null;
        }
    }

    private static final class Lexicon {

        private final Map<LexiconKey, Lexeme> exact;
        private final Map<String, List<Lexeme>> byLemma;
        private final Map<String, Integer> cefrCounts;
        private final int maxRank;
        private final int rowCount;

        private Lexicon(
                Map<LexiconKey, Lexeme> exact,
                Map<String, List<Lexeme>> byLemma,
                Map<String, Integer> cefrCounts,
                int maxRank,
                int rowCount
        ) {
            this.exact = exact;
            this.byLemma = byLemma;
            this.cefrCounts = cefrCounts;
            this.maxRank = maxRank;
            this.rowCount = rowCount;
        }

        private static Lexicon read(byte[] compressed) throws IOException {
            Map<LexiconKey, Lexeme> exact = new HashMap<>();
            Map<String, List<Lexeme>> byLemma = new HashMap<>();
            Map<String, Integer> cefrCounts = new HashMap<>();
            int maxRank = 0;
            int rowCount = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new ByteArrayInputStream(compressed)),
                    StandardCharsets.UTF_8
            ))) {
                String header = reader.readLine();
                if (!"lemma\tpos\tfrequency_rank\tfrequency_count\tcefr_estimate\tcefr_source".equals(header)) {
                    throw new IllegalArgumentException("Unexpected wordhoard TSV header: " + header);
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split("\t", -1);
                    if (values.length != 6) {
                        throw new IllegalArgumentException("Unexpected wordhoard row: " + line);
                    }
                    Lexeme lexeme = new Lexeme(
                            normalize(values[0]),
                            UniversalPos.valueOf(values[1]),
                            Integer.parseInt(values[2]),
                            Long.parseLong(values[3]),
                            values[4],
                            values[5]
                    );
                    LexiconKey key = new LexiconKey(lexeme.lemma(), lexeme.pos());
                    exact.merge(key, lexeme, (left, right) -> left.rank() <= right.rank() ? left : right);
                    byLemma.computeIfAbsent(lexeme.lemma(), ignored -> new ArrayList<>()).add(lexeme);
                    cefrCounts.merge(lexeme.cefr(), 1, Integer::sum);
                    maxRank = Math.max(maxRank, lexeme.rank());
                    rowCount++;
                }
            }
            byLemma.values().forEach(values -> values.sort(Comparator.comparingInt(Lexeme::rank)));
            return new Lexicon(exact, byLemma, cefrCounts, maxRank, rowCount);
        }

        private Optional<Lexeme> lookup(String lemma, UniversalPos pos) {
            return Optional.ofNullable(exact.get(new LexiconKey(normalize(lemma), pos)));
        }

        private Resolution resolve(String lemma, UniversalPos pos, String surface) {
            Optional<Lexeme> exactResult = lookup(lemma, pos);
            if (exactResult.isPresent()) {
                return new Resolution(exactResult, ResolutionKind.EXACT);
            }
            List<Lexeme> lemmaResults = contentEntries(normalize(lemma));
            if (!lemmaResults.isEmpty()) {
                return new Resolution(Optional.of(lemmaResults.getFirst()), ResolutionKind.POS_FALLBACK);
            }
            String normalizedSurface = normalize(surface);
            if (!normalizedSurface.equals(normalize(lemma))) {
                Optional<Lexeme> surfaceExact = lookup(normalizedSurface, pos);
                if (surfaceExact.isPresent()) {
                    return new Resolution(surfaceExact, ResolutionKind.SURFACE_FALLBACK);
                }
                List<Lexeme> surfaceResults = contentEntries(normalizedSurface);
                if (!surfaceResults.isEmpty()) {
                    return new Resolution(Optional.of(surfaceResults.getFirst()), ResolutionKind.SURFACE_FALLBACK);
                }
            }
            return new Resolution(Optional.empty(), ResolutionKind.UNKNOWN);
        }

        private List<Lexeme> contentEntries(String lemma) {
            return byLemma.getOrDefault(lemma, List.of()).stream()
                    .filter(value -> value.pos() != UniversalPos.PROPN)
                    .toList();
        }

        private int size() {
            return rowCount;
        }

        private int uniqueKeyCount() {
            return exact.size();
        }

        private int maxRank() {
            return maxRank;
        }

        private Map<String, Integer> cefrCounts() {
            return Map.copyOf(cefrCounts);
        }
    }

    private static int cefrOrdinal(String cefr) {
        return switch (cefr) {
            case "A1" -> 0;
            case "A2" -> 1;
            case "B1" -> 2;
            case "B2" -> 3;
            case "C1" -> 4;
            case "C2" -> 5;
            default -> throw new IllegalArgumentException("Unknown CEFR proxy: " + cefr);
        };
    }

    private enum UniversalPos {
        NOUN,
        PROPN,
        VERB,
        ADV,
        ADJ,
        PRON,
        ADP,
        INTJ,
        DET,
        CCONJ,
        AUX,
        SCONJ,
        PART
    }

    private enum ResolutionKind {
        EXACT,
        POS_FALLBACK,
        SURFACE_FALLBACK,
        UNKNOWN
    }

    private enum Direction {
        IMPROVED("leichter"),
        TIED("unentschieden"),
        WORSENED("schwerer");

        private final String label;

        Direction(String label) {
            this.label = label;
        }

        private static Direction fromDelta(double delta) {
            if (delta >= MIN_PAIR_DIFFICULTY_DELTA) {
                return IMPROVED;
            }
            if (delta <= -MIN_PAIR_DIFFICULTY_DELTA) {
                return WORSENED;
            }
            return TIED;
        }
    }

    private enum Verdict {
        ROBUST_LEXICAL_SIGNAL(
                "Robustes lexikalisches Signal im Testkorpus",
                "Der Frequenzrang erkennt die lexikalische Richtung in diesem kleinen, selbst verfassten Korpus ausreichend oft und deckt genügend Inhaltswörter ab."
        ),
        LIMITED_DIAGNOSTIC_SIGNAL(
                "Begrenztes Diagnosesignal",
                "Die Daten liefern Hinweise auf schwierige Wörter, sind für eine alleinige Qualitätsentscheidung aber zu lückenhaft oder zu richtungsunsicher."
        ),
        NOT_SUFFICIENT(
                "Nicht ausreichend",
                "Richtung oder Abdeckung genügen selbst für ein begrenztes lexikalisches Diagnosesignal nicht."
        );

        private final String label;
        private final String explanation;

        Verdict(String label, String explanation) {
            this.label = label;
            this.explanation = explanation;
        }
    }

    private static final class MutableTermSignal {
        private final String lemma;
        private final Lexeme lexeme;
        private int count;

        private MutableTermSignal(String lemma, Lexeme lexeme) {
            this.lemma = lemma;
            this.lexeme = lexeme;
        }

        private TermSignal freeze() {
            return new TermSignal(
                    lemma,
                    count,
                    lexeme == null ? null : lexeme.rank(),
                    lexeme == null ? null : lexeme.cefr(),
                    lexeme == null ? null : lexeme.cefrSource()
            );
        }
    }

    private record Manifest(int schemaVersion, Source source, Transformation transformation) {
    }

    private record Source(
            String repository,
            String release,
            String commit,
            String archiveUrl,
            String archiveSha256,
            String csvName,
            String dataLicense
    ) {
    }

    private record Transformation(
            String description,
            int rowCount,
            List<String> columns,
            String lexiconSha256,
            Map<String, Integer> cefrCounts
    ) {
    }

    private record ReferenceCase(
            String id,
            String pairId,
            String role,
            String category,
            String text,
            int wordCount,
            double zixScore
    ) {
    }

    private record LexiconKey(String lemma, UniversalPos pos) {
    }

    private record Lexeme(
            String lemma,
            UniversalPos pos,
            int rank,
            long frequency,
            String cefr,
            String cefrSource
    ) {
    }

    private record Resolution(Optional<Lexeme> lexeme, ResolutionKind kind) {
    }

    private record TokenAnalysis(String surface, String lemma, UniversalPos pos) {
    }

    private record TokenClassification(TokenAnalysis analysis, boolean properName, boolean hasTaggedReading) {
    }

    private record TermSignal(String lemma, int occurrences, Integer rank, String cefr, String cefrSource) {
    }

    private record LexicalMeasurement(
            int contentTokens,
            int knownTokens,
            int unknownTokens,
            int rareTokens,
            int properNameTokens,
            int excludedNumberTokens,
            int posFallbacks,
            int surfaceFallbacks,
            double knownCoverage,
            double unknownShare,
            double rareShare,
            double estimatedB1Coverage,
            double estimatedB2Coverage,
            double lexicalDifficulty,
            Integer medianKnownRank,
            List<TermSignal> difficultTerms
    ) {
    }

    private record CaseResult(ReferenceCase reference, LexicalMeasurement measurement) {
    }

    private record PairResult(
            String pairId,
            String category,
            double difficultyDelta,
            double b1CoverageDelta,
            double b2CoverageDelta,
            double knownCoverageDelta,
            double zixDelta,
            boolean zixDecided,
            boolean sameDirectionAsZix,
            Direction direction
    ) {
    }

    private record AdministrationTermResult(
            String term,
            String expectedLemma,
            String expectedPos,
            boolean directlyKnown,
            Integer rank,
            String cefr,
            String cefrSource,
            String languageToolLemma,
            String languageToolPos,
            boolean languageToolLookupKnown
    ) {
    }

    private record Summary(
            Verdict verdict,
            int eligibleCaseCount,
            int diagnosticCaseCount,
            int pairCount,
            int improvedPairs,
            int worsenedPairs,
            int tiedPairs,
            double improvementRate,
            double worseningRate,
            double medianKnownCoverage,
            double medianUnknownShare,
            int eligibleContentTokens,
            double posFallbackRate,
            double surfaceFallbackRate,
            int zixDecidedPairs,
            double zixDirectionAgreement
    ) {
    }

    private record ExperimentReport(
            Summary summary,
            List<PairResult> pairs,
            List<CaseResult> cases,
            List<AdministrationTermResult> administrationTerms
    ) {
    }
}
