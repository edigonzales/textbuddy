package app.textbuddy.zix;

import org.junit.jupiter.api.Test;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;
import org.languagetool.language.SwissGerman;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolated experiment: compare a LanguageTool feature approximation with pinned ZIX golden data.
 * This class deliberately lives in test code and is excluded from the normal test/check lifecycle.
 */
class ZixCompatibilityTest {

    private static final String RESOURCE_ROOT = "/zix-compatibility/";
    private static final String PINNED_COMMIT = "3cd7e7e9fd0937e1c41e2bf0e040950172ab3a6e";
    private static final List<String> FEATURES = List.of(
            "sentence_length_mean",
            "rix",
            "vocab_a1",
            "vocab_a2",
            "vocab_b1",
            "common_word_score"
    );
    private static final double MAX_MAE = 0.5;
    private static final double MAX_P95 = 1.0;
    private static final double MIN_CEFR_MATCH = 0.90;
    private static final double MIN_ZIX_DIRECTION = 0.95;
    private static final double MIN_ZIX_SPEARMAN = 0.95;
    private static final double MIN_PROXY_DIRECTION = 0.90;
    private static final double MIN_PROXY_SPEARMAN = 0.90;
    private static final double MIN_OFFICIAL_DIRECTION_DELTA = 0.5;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void comparesLanguageToolApproximationWithPinnedZix() throws Exception {
        ReferenceData reference = readJson("reference.json", ReferenceData.class);
        ModelData model = readJson("model.json", ModelData.class);
        validateFixtures(reference, model);

        ZixPrototype prototype = new ZixPrototype(
                readCefrVocabulary(),
                readCommonWordScores(),
                model,
                new JLanguageTool(new SwissGerman())
        );
        verifyPreprocessing();
        verifyModelAndCefr(reference, prototype);

        List<CaseResult> cases = new ArrayList<>();
        for (ReferenceCase golden : reference.cases()) {
            Measurement javaMeasurement = prototype.measure(golden.text());
            cases.add(compare(golden, javaMeasurement));
        }

        assertThat(prototype.measure(reference.cases().getFirst().text()))
                .isEqualTo(cases.getFirst().javaMeasurement());
        for (CaseResult result : cases) {
            for (double value : result.javaMeasurement().features().values()) {
                assertThat(value).isFinite();
            }
        }

        CompatibilityReport report = evaluate(reference, cases);
        writeReports(reference, report);

        assertThat(report.summary().eligibleCaseCount()).isEqualTo(40);
        assertThat(report.summary().diagnosticCaseCount()).isEqualTo(12);
        assertThat(report.pairs()).hasSize(20);
    }

    private static void validateFixtures(ReferenceData reference, ModelData model) {
        assertThat(reference.schemaVersion()).isEqualTo(1);
        assertThat(reference.source().commit()).isEqualTo(PINNED_COMMIT);
        assertThat(reference.source().zixVersion()).isEqualTo("0.2.1");
        assertThat(reference.source().spacyModelVersion()).isEqualTo("3.8.0");
        assertThat(reference.featureOrder()).containsExactlyElementsOf(FEATURES);
        assertThat(model.featureOrder()).containsExactlyElementsOf(FEATURES);
        assertThat(model.sourceCommit()).isEqualTo(PINNED_COMMIT);
        assertThat(model.scalerMean()).hasSize(FEATURES.size());
        assertThat(model.scalerScale()).hasSize(FEATURES.size());
        assertThat(model.ridgeCoefficients()).hasSize(FEATURES.size());

        List<ReferenceCase> paired = reference.cases().stream()
                .filter(item -> item.pairId() != null)
                .toList();
        assertThat(paired).hasSize(40)
                .allSatisfy(item -> assertThat(item.wordCount()).isGreaterThanOrEqualTo(20));
        assertThat(reference.cases().stream().filter(item -> item.pairId() == null)).hasSize(12);
        assertThat(paired.stream().map(ReferenceCase::pairId).distinct()).hasSize(20);
        assertThat(reference.cases().stream()
                .filter(item -> item.pairId() == null)
                .map(ReferenceCase::category))
                .contains("short", "ss-sharp-s", "lists", "numbers", "proper-names");
    }

    private static void verifyPreprocessing() {
        assertThat(ZixPrototype.preprocess("  Erster Punkt  \n\n- Zweiter   Punkt\n• Dritter Punkt? "))
                .isEqualTo("Erster Punkt. Zweiter Punkt. Dritter Punkt?");
        assertThat(ZixPrototype.preprocess("Bereits fertig!\r\nNoch offen"))
                .isEqualTo("Bereits fertig! Noch offen.");
        assertThat(ZixPrototype.preprocess("Die Straße ist gross"))
                .isEqualTo("Die Straße ist gross.");
    }

    private static void verifyModelAndCefr(ReferenceData reference, ZixPrototype prototype) {
        for (ReferenceCase golden : reference.cases()) {
            double reconstructed = prototype.score(Features.from(golden.features()));
            assertThat(reconstructed).isCloseTo(golden.zixScore(), within(1e-10));
            assertThat(ZixPrototype.cefr(golden.zixScore())).isEqualTo(golden.cefrBand());
        }
        assertThat(ZixPrototype.cefr(4.0)).isEqualTo("A1");
        assertThat(ZixPrototype.cefr(2.0)).isEqualTo("A2");
        assertThat(ZixPrototype.cefr(0.0)).isEqualTo("B1");
        assertThat(ZixPrototype.cefr(-2.0)).isEqualTo("B2");
        assertThat(ZixPrototype.cefr(-4.0)).isEqualTo("C1");
        assertThat(ZixPrototype.cefr(-4.00001)).isEqualTo("C2");
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

    private static CaseResult compare(ReferenceCase golden, Measurement javaMeasurement) {
        Map<String, Double> featureDifferences = new LinkedHashMap<>();
        Map<String, Double> javaFeatures = javaMeasurement.features().asMap();
        FEATURES.forEach(feature -> featureDifferences.put(
                feature,
                Math.abs(golden.features().get(feature) - javaFeatures.get(feature))
        ));
        String javaBand = ZixPrototype.cefr(javaMeasurement.score());
        return new CaseResult(
                golden,
                javaMeasurement,
                javaBand,
                Math.abs(golden.zixScore() - javaMeasurement.score()),
                golden.cefrBand().equals(javaBand),
                featureDifferences
        );
    }

    private static CompatibilityReport evaluate(ReferenceData reference, List<CaseResult> cases) {
        List<CaseResult> eligible = cases.stream()
                .filter(result -> result.golden().wordCount() >= 20)
                .toList();
        List<Double> scoreErrors = eligible.stream().map(CaseResult::scoreDifference).toList();
        double mae = mean(scoreErrors);
        double p95 = percentile95(scoreErrors);
        double cefrMatch = ratio(eligible.stream().filter(CaseResult::sameCefrBand).count(), eligible.size());
        double spearman = spearman(
                eligible.stream().mapToDouble(result -> result.golden().zixScore()).toArray(),
                eligible.stream().mapToDouble(result -> result.javaMeasurement().score()).toArray()
        );

        Map<String, List<CaseResult>> byPair = new LinkedHashMap<>();
        for (CaseResult result : cases) {
            if (result.golden().pairId() != null) {
                byPair.computeIfAbsent(result.golden().pairId(), ignored -> new ArrayList<>()).add(result);
            }
        }
        List<PairResult> pairs = byPair.entrySet().stream()
                .map(entry -> comparePair(entry.getKey(), entry.getValue()))
                .toList();
        List<PairResult> decidedPairs = pairs.stream().filter(PairResult::decided).toList();
        double directionMatch = ratio(
                decidedPairs.stream().filter(PairResult::sameDirection).count(),
                decidedPairs.size()
        );
        long strongInversions = pairs.stream().filter(PairResult::strongInversion).count();

        Verdict verdict;
        if (mae <= MAX_MAE
                && p95 <= MAX_P95
                && cefrMatch >= MIN_CEFR_MATCH
                && directionMatch >= MIN_ZIX_DIRECTION
                && spearman >= MIN_ZIX_SPEARMAN
                && strongInversions == 0) {
            verdict = Verdict.ZIX_COMPATIBLE;
        } else if (directionMatch >= MIN_PROXY_DIRECTION
                && spearman >= MIN_PROXY_SPEARMAN
                && strongInversions == 0) {
            verdict = Verdict.TEXTBUDDY_PROXY_CANDIDATE;
        } else {
            verdict = Verdict.INSUFFICIENT;
        }

        Map<String, FeatureSummary> featureSummaries = new LinkedHashMap<>();
        for (String feature : FEATURES) {
            List<Double> errors = eligible.stream()
                    .map(result -> result.featureDifferences().get(feature))
                    .toList();
            featureSummaries.put(feature, new FeatureSummary(
                    mean(errors),
                    percentile95(errors),
                    errors.stream().mapToDouble(Double::doubleValue).max().orElse(0.0),
                    featureCause(feature)
            ));
        }

        Summary resultSummary = new Summary(
                verdict,
                eligible.size(),
                cases.size() - eligible.size(),
                mae,
                p95,
                cefrMatch,
                decidedPairs.size(),
                directionMatch,
                spearman,
                strongInversions
        );
        return new CompatibilityReport(
                resultSummary,
                featureSummaries,
                cases,
                pairs,
                cases.stream().filter(result -> !result.sameCefrBand()).toList()
        );
    }

    private static PairResult comparePair(String pairId, List<CaseResult> values) {
        CaseResult original = values.stream()
                .filter(value -> "original".equals(value.golden().role()))
                .findFirst()
                .orElseThrow();
        CaseResult simplified = values.stream()
                .filter(value -> "simplified".equals(value.golden().role()))
                .findFirst()
                .orElseThrow();
        double officialDelta = simplified.golden().zixScore() - original.golden().zixScore();
        double javaDelta = simplified.javaMeasurement().score() - original.javaMeasurement().score();
        boolean decided = Math.abs(officialDelta) >= MIN_OFFICIAL_DIRECTION_DELTA;
        boolean sameDirection = !decided || Math.signum(officialDelta) == Math.signum(javaDelta);
        boolean strongInversion = officialDelta >= 1.0 && javaDelta <= -0.5;
        return new PairResult(pairId, officialDelta, javaDelta, decided, sameDirection, strongInversion);
    }

    private static String featureCause(String feature) {
        return switch (feature) {
            case "sentence_length_mean" -> "Abweichende Satz- und Token-Grenzen von LanguageTool und spaCy.";
            case "rix" -> "Abweichende Satzgrenzen sowie Tokenisierung bei Komposita, Zahlen und Eigennamen.";
            case "vocab_a1", "vocab_a2", "vocab_b1" ->
                    "Abweichende Lemma-Auswahl und Erkennung von Zahlen verändern Treffer und Nenner.";
            case "common_word_score" ->
                    "Abweichende Lemma-Auswahl und Zahlenerkennung verändern Wortfrequenzsumme und Nenner.";
            default -> throw new IllegalArgumentException("Unknown feature: " + feature);
        };
    }

    private static void writeReports(ReferenceData reference, CompatibilityReport report) throws IOException {
        Path reportDir = Path.of(System.getProperty("zix.reportDir", "build/reports/zix-compatibility"));
        Files.createDirectories(reportDir);
        Map<String, Object> jsonReport = new LinkedHashMap<>();
        jsonReport.put("generatedAt", Instant.now().toString());
        jsonReport.put("reference", reference.source());
        jsonReport.put("languageToolVersion", "6.7");
        jsonReport.put("language", "SwissGerman");
        jsonReport.put("thresholds", Map.of(
                "zixMaxMae", MAX_MAE,
                "zixMaxP95", MAX_P95,
                "zixMinCefrMatch", MIN_CEFR_MATCH,
                "zixMinDirectionMatch", MIN_ZIX_DIRECTION,
                "zixMinSpearman", MIN_ZIX_SPEARMAN,
                "proxyMinDirectionMatch", MIN_PROXY_DIRECTION,
                "proxyMinSpearman", MIN_PROXY_SPEARMAN,
                "minimumOfficialDirectionDelta", MIN_OFFICIAL_DIRECTION_DELTA
        ));
        jsonReport.put("summary", report.summary());
        jsonReport.put("featureDifferences", report.featureSummaries());
        jsonReport.put("pairs", report.pairs());
        jsonReport.put("cefrCrossings", report.cefrCrossings().stream().map(ZixCompatibilityTest::caseMap).toList());
        jsonReport.put("cases", report.cases().stream().map(ZixCompatibilityTest::caseMap).toList());
        JSON.writerWithDefaultPrettyPrinter().writeValue(reportDir.resolve("report.json").toFile(), jsonReport);
        Files.writeString(reportDir.resolve("report.md"), markdown(reference, report), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> caseMap(CaseResult result) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", result.golden().id());
        row.put("pairId", result.golden().pairId());
        row.put("role", result.golden().role());
        row.put("category", result.golden().category());
        row.put("wordCount", result.golden().wordCount());
        row.put("eligible", result.golden().wordCount() >= 20);
        row.put("officialFeatures", result.golden().features());
        row.put("javaFeatures", result.javaMeasurement().features().asMap());
        row.put("featureAbsoluteDifferences", result.featureDifferences());
        row.put("officialZix", result.golden().zixScore());
        row.put("javaScore", result.javaMeasurement().score());
        row.put("scoreAbsoluteDifference", result.scoreDifference());
        row.put("officialCefr", result.golden().cefrBand());
        row.put("javaCefr", result.javaBand());
        return row;
    }

    private static String markdown(ReferenceData reference, CompatibilityReport report) {
        Summary summary = report.summary();
        StringBuilder out = new StringBuilder("# ZIX-/LanguageTool-Kompatibilitätsbericht\n\n");
        out.append("Referenz: ZIX ").append(reference.source().zixVersion())
                .append(" (`").append(reference.source().commit()).append("`), ")
                .append(reference.source().spacyModel()).append(' ')
                .append(reference.source().spacyModelVersion()).append(". Java: LanguageTool 6.7 mit `SwissGerman`.\n\n");
        out.append("## Urteil\n\n**").append(summary.verdict().label()).append("**\n\n");
        out.append("| Messgrösse | Ergebnis | ZIX-Grenze | Proxy-Grenze |\n")
                .append("| --- | ---: | ---: | ---: |\n")
                .append(row("ZIX-MAE", summary.mae(), MAX_MAE, null))
                .append(row("ZIX-95%-Quantil", summary.p95(), MAX_P95, null))
                .append(rowPercent("Identische CEFR-Bänder", summary.cefrMatch(), MIN_CEFR_MATCH, null))
                .append(rowPercent("Richtungsübereinstimmung", summary.directionMatch(), MIN_ZIX_DIRECTION, MIN_PROXY_DIRECTION))
                .append(row("Spearman-Korrelation", summary.spearman(), MIN_ZIX_SPEARMAN, MIN_PROXY_SPEARMAN))
                .append("| Starke Umkehrungen | ").append(summary.strongInversions()).append(" | 0 | 0 |\n\n");
        out.append(summary.eligibleCaseCount()).append(" Texte mit mindestens 20 Wörtern fliessen in das Urteil ein. ")
                .append(summary.diagnosticCaseCount()).append(" kürzere Randfälle werden nur diagnostisch ausgewiesen. Von ")
                .append(report.pairs().size()).append(" Paaren waren ").append(summary.decidedPairCount())
                .append(" anhand eines offiziellen Deltas von mindestens 0.5 richtungsentscheidend.\n\n");

        out.append("## Abweichungen der sechs Merkmale\n\n")
                .append("| Merkmal | MAE | 95%-Quantil | Maximum | Wahrscheinliche Ursache |\n")
                .append("| --- | ---: | ---: | ---: | --- |\n");
        report.featureSummaries().forEach((feature, values) -> out.append("| ")
                .append(feature).append(" | ").append(format(values.mae()))
                .append(" | ").append(format(values.p95()))
                .append(" | ").append(format(values.maximum()))
                .append(" | ").append(values.cause()).append(" |\n"));

        out.append("\n## Paarrichtung\n\n")
                .append("| Paar | offizielles Delta | Java-Delta | Bewertung |\n")
                .append("| --- | ---: | ---: | --- |\n");
        for (PairResult pair : report.pairs()) {
            String assessment = !pair.decided() ? "unentschieden"
                    : pair.strongInversion() ? "starke Umkehrung"
                    : pair.sameDirection() ? "gleich" : "umgekehrt";
            out.append("| ").append(pair.pairId()).append(" | ")
                    .append(format(pair.officialDelta())).append(" | ")
                    .append(format(pair.javaDelta())).append(" | ")
                    .append(assessment).append(" |\n");
        }

        out.append("\n## CEFR-Grenzübertritte\n\n");
        if (report.cefrCrossings().isEmpty()) {
            out.append("Keine.\n");
        } else {
            out.append("| Text | Wörter | offiziell | Java | |Δ| |\n")
                    .append("| --- | ---: | --- | --- | ---: |\n");
            for (CaseResult crossing : report.cefrCrossings()) {
                out.append("| ").append(crossing.golden().id()).append(" | ")
                        .append(crossing.golden().wordCount()).append(" | ")
                        .append(crossing.golden().cefrBand()).append(" | ")
                        .append(crossing.javaBand()).append(" | ")
                        .append(format(crossing.scoreDifference())).append(" |\n");
            }
        }

        out.append("\n## Diagnosefälle unter 20 Wörtern\n\n")
                .append("| Text | Kategorie | Wörter | offiziell | Java | |Δ| |\n")
                .append("| --- | --- | ---: | ---: | ---: | ---: |\n");
        report.cases().stream()
                .filter(result -> result.golden().wordCount() < 20)
                .forEach(result -> out.append("| ").append(result.golden().id()).append(" | ")
                        .append(result.golden().category()).append(" | ")
                        .append(result.golden().wordCount()).append(" | ")
                        .append(format(result.golden().zixScore())).append(" | ")
                        .append(format(result.javaMeasurement().score())).append(" | ")
                        .append(format(result.scoreDifference())).append(" |\n"));
        return out.toString();
    }

    private static String row(String label, double result, Double strict, Double proxy) {
        return "| " + label + " | " + format(result) + " | "
                + (strict == null ? "–" : format(strict)) + " | "
                + (proxy == null ? "–" : format(proxy)) + " |\n";
    }

    private static String rowPercent(String label, double result, Double strict, Double proxy) {
        return "| " + label + " | " + formatPercent(result) + " | "
                + (strict == null ? "–" : formatPercent(strict)) + " | "
                + (proxy == null ? "–" : formatPercent(proxy)) + " |\n";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f %%", value * 100.0);
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 1.0 : (double) numerator / denominator;
    }

    private static double percentile95(List<Double> values) {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int index = Math.max(0, (int) Math.ceil(sorted.size() * 0.95) - 1);
        return sorted.get(index);
    }

    private static double spearman(double[] left, double[] right) {
        return pearson(ranks(left), ranks(right));
    }

    private static double[] ranks(double[] values) {
        Integer[] order = new Integer[values.length];
        Arrays.setAll(order, index -> index);
        Arrays.sort(order, Comparator.comparingDouble(index -> values[index]));
        double[] ranks = new double[values.length];
        int start = 0;
        while (start < order.length) {
            int end = start + 1;
            while (end < order.length
                    && Double.compare(values[order[start]], values[order[end]]) == 0) {
                end++;
            }
            double rank = (start + 1 + end) / 2.0;
            for (int index = start; index < end; index++) {
                ranks[order[index]] = rank;
            }
            start = end;
        }
        return ranks;
    }

    private static double pearson(double[] left, double[] right) {
        double leftMean = Arrays.stream(left).average().orElseThrow();
        double rightMean = Arrays.stream(right).average().orElseThrow();
        double numerator = 0.0;
        double leftSquares = 0.0;
        double rightSquares = 0.0;
        for (int index = 0; index < left.length; index++) {
            double leftDelta = left[index] - leftMean;
            double rightDelta = right[index] - rightMean;
            numerator += leftDelta * rightDelta;
            leftSquares += leftDelta * leftDelta;
            rightSquares += rightDelta * rightDelta;
        }
        return numerator / Math.sqrt(leftSquares * rightSquares);
    }

    private static <T> T readJson(String name, Class<T> type) throws IOException {
        try (InputStream input = resource(name)) {
            return JSON.readValue(input, type);
        }
    }

    private static CefrVocabulary readCefrVocabulary() throws IOException {
        Set<String> a1 = new HashSet<>();
        Set<String> a2 = new HashSet<>();
        Set<String> b1 = new HashSet<>();
        try (BufferedReader reader = reader("cefr-vocabulary.tsv")) {
            reader.readLine();
            for (String line; (line = reader.readLine()) != null; ) {
                String[] columns = line.split("\t", -1);
                switch (columns[1]) {
                    case "A1" -> a1.add(columns[0]);
                    case "A2" -> a2.add(columns[0]);
                    case "B1" -> b1.add(columns[0]);
                    default -> throw new IllegalStateException("Unexpected CEFR level: " + columns[1]);
                }
            }
        }
        return new CefrVocabulary(a1, a2, b1);
    }

    private static Map<String, Double> readCommonWordScores() throws IOException {
        Map<String, Double> scores = new HashMap<>();
        try (BufferedReader reader = reader("common-word-scores.tsv")) {
            reader.readLine();
            for (String line; (line = reader.readLine()) != null; ) {
                String[] columns = line.split("\t", -1);
                scores.put(columns[0], Double.parseDouble(columns[1]));
            }
        }
        return scores;
    }

    private static BufferedReader reader(String name) {
        return new BufferedReader(new InputStreamReader(resource(name), StandardCharsets.UTF_8));
    }

    private static InputStream resource(String name) {
        InputStream stream = ZixCompatibilityTest.class.getResourceAsStream(RESOURCE_ROOT + name);
        if (stream == null) {
            throw new IllegalStateException("Missing test resource: " + name);
        }
        return stream;
    }

    private static final class ZixPrototype {

        private static final Pattern NUMERIC = Pattern.compile(
                "[+-]?(?:\\d+(?:[.,'’:/-]\\d+)*)%?",
                Pattern.UNICODE_CHARACTER_CLASS
        );
        private static final Set<String> NUMBER_WORDS = Set.of(
                "null", "eins", "ein", "eine", "einen", "einem", "einer", "eines",
                "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn",
                "elf", "zwölf", "dreizehn", "vierzehn", "fünfzehn", "sechzehn", "siebzehn",
                "achtzehn", "neunzehn", "zwanzig", "dreissig", "vierzig", "fünfzig", "sechzig",
                "siebzig", "achtzig", "neunzig", "hundert", "tausend", "million", "millionen",
                "milliarde", "milliarden"
        );

        private final CefrVocabulary vocabulary;
        private final Map<String, Double> wordScores;
        private final ModelData model;
        private final JLanguageTool languageTool;

        private ZixPrototype(
                CefrVocabulary vocabulary,
                Map<String, Double> wordScores,
                ModelData model,
                JLanguageTool languageTool
        ) {
            this.vocabulary = vocabulary;
            this.wordScores = wordScores;
            this.model = model;
            this.languageTool = languageTool;
        }

        private Measurement measure(String source) throws IOException {
            String text = preprocess(source);
            List<AnalyzedSentence> sentences = languageTool.analyzeText(text);
            int sentenceCount = 0;
            int sentenceTokenTotal = 0;
            int longWords = 0;
            int documentLength = 0;
            int a1 = 0;
            int a2 = 0;
            int b1 = 0;
            double commonScore = 0.0;

            for (AnalyzedSentence sentence : sentences) {
                int sentenceLength = 0;
                for (AnalyzedTokenReadings token : sentence.getTokensWithoutWhitespace()) {
                    String surface = token.getToken();
                    if (token.isSentenceStart() || surface == null || surface.isBlank()) {
                        continue;
                    }
                    boolean punctuation = isPunctuation(surface);
                    if (!punctuation) {
                        sentenceLength++;
                    }
                    if (surface.codePointCount(0, surface.length()) > 6) {
                        longWords++;
                    }
                    if (punctuation || isNumeric(surface)) {
                        continue;
                    }

                    documentLength++;
                    String lemma = lemma(token, surface);
                    if (vocabulary.a1().contains(lemma)) {
                        a1++;
                        a2++;
                        b1++;
                    } else if (vocabulary.a2().contains(lemma)) {
                        a2++;
                        b1++;
                    } else if (vocabulary.b1().contains(lemma)) {
                        b1++;
                    }
                    commonScore += wordScores.getOrDefault(lemma, 0.0);
                }
                if (sentenceLength > 0) {
                    sentenceCount++;
                    sentenceTokenTotal += sentenceLength;
                }
            }
            if (documentLength == 0 || sentenceCount == 0) {
                throw new IllegalArgumentException("Text has no measurable words");
            }
            Features features = new Features(
                    (double) sentenceTokenTotal / sentenceCount,
                    (double) longWords / sentenceCount,
                    (double) a1 / documentLength,
                    (double) a2 / documentLength,
                    (double) b1 / documentLength,
                    commonScore / documentLength / 1000.0
            );
            return new Measurement(text, features, score(features));
        }

        private double score(Features features) {
            double[] raw = features.values();
            double prediction = model.ridgeIntercept();
            for (int index = 0; index < raw.length; index++) {
                double scaled = (raw[index] - model.scalerMean().get(index))
                        / model.scalerScale().get(index);
                prediction += model.ridgeCoefficients().get(index) * scaled;
            }
            ScoreTransform transform = model.scoreTransform();
            double score = (transform.predictionOffset() - prediction)
                    * transform.multiplier() + transform.shift();
            return Math.max(transform.minimum(), Math.min(transform.maximum(), score));
        }

        private static String preprocess(String text) {
            List<String> normalized = new ArrayList<>();
            for (String sourceLine : text.split("\\R", -1)) {
                String line = sourceLine.strip();
                if (line.isEmpty()) {
                    continue;
                }
                char last = line.charAt(line.length() - 1);
                if (last != '.' && last != '?' && last != '!') {
                    line += ".";
                }
                if (line.charAt(0) == '-' || line.charAt(0) == '•') {
                    line = line.substring(1).strip();
                }
                normalized.add(line.replaceAll("\\s+", " "));
            }
            return String.join(" ", normalized);
        }

        private static String lemma(AnalyzedTokenReadings readings, String surface) {
            for (AnalyzedToken reading : readings) {
                String lemma = reading.getLemma();
                if (lemma != null && !lemma.isBlank()) {
                    return normalize(lemma);
                }
            }
            return normalize(surface);
        }

        private static String normalize(String value) {
            return Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.GERMAN);
        }

        private static boolean isNumeric(String surface) {
            String normalized = normalize(surface);
            return NUMERIC.matcher(normalized).matches() || NUMBER_WORDS.contains(normalized);
        }

        private static boolean isPunctuation(String value) {
            return !value.isEmpty() && value.codePoints().allMatch(ZixPrototype::isPunctuationCodePoint);
        }

        private static boolean isPunctuationCodePoint(int codePoint) {
            return switch (Character.getType(codePoint)) {
                case Character.CONNECTOR_PUNCTUATION,
                        Character.DASH_PUNCTUATION,
                        Character.START_PUNCTUATION,
                        Character.END_PUNCTUATION,
                        Character.INITIAL_QUOTE_PUNCTUATION,
                        Character.FINAL_QUOTE_PUNCTUATION,
                        Character.OTHER_PUNCTUATION -> true;
                default -> false;
            };
        }

        private static String cefr(double score) {
            if (score >= 4.0) {
                return "A1";
            }
            if (score >= 2.0) {
                return "A2";
            }
            if (score >= 0.0) {
                return "B1";
            }
            if (score >= -2.0) {
                return "B2";
            }
            if (score >= -4.0) {
                return "C1";
            }
            return "C2";
        }
    }

    private record ReferenceData(
            int schemaVersion,
            Source source,
            List<String> featureOrder,
            List<ReferenceCase> cases
    ) {
    }

    private record Source(
            String repository,
            String commit,
            String zixVersion,
            String spacyModel,
            String spacyModelVersion,
            String lockFile
    ) {
    }

    private record ReferenceCase(
            String id,
            String pairId,
            String role,
            String category,
            String text,
            int wordCount,
            String normalizedText,
            Map<String, Double> features,
            double zixScore,
            String cefrBand
    ) {
    }

    private record ModelData(
            int schemaVersion,
            List<String> featureOrder,
            List<Double> scalerMean,
            List<Double> scalerScale,
            List<Double> ridgeCoefficients,
            double ridgeIntercept,
            ScoreTransform scoreTransform,
            String sourceCommit,
            String zixVersion
    ) {
    }

    private record ScoreTransform(
            double predictionOffset,
            double multiplier,
            double shift,
            double minimum,
            double maximum
    ) {
    }

    private record CefrVocabulary(Set<String> a1, Set<String> a2, Set<String> b1) {
    }

    private record Features(
            double sentenceLengthMean,
            double rix,
            double vocabA1,
            double vocabA2,
            double vocabB1,
            double commonWordScore
    ) {
        private static Features from(Map<String, Double> values) {
            return new Features(
                    values.get("sentence_length_mean"),
                    values.get("rix"),
                    values.get("vocab_a1"),
                    values.get("vocab_a2"),
                    values.get("vocab_b1"),
                    values.get("common_word_score")
            );
        }

        private double[] values() {
            return new double[]{
                    sentenceLengthMean,
                    rix,
                    vocabA1,
                    vocabA2,
                    vocabB1,
                    commonWordScore
            };
        }

        private Map<String, Double> asMap() {
            Map<String, Double> values = new LinkedHashMap<>();
            for (int index = 0; index < FEATURES.size(); index++) {
                values.put(FEATURES.get(index), values()[index]);
            }
            return values;
        }
    }

    private record Measurement(String normalizedText, Features features, double score) {
    }

    private record CaseResult(
            ReferenceCase golden,
            Measurement javaMeasurement,
            String javaBand,
            double scoreDifference,
            boolean sameCefrBand,
            Map<String, Double> featureDifferences
    ) {
    }

    private record PairResult(
            String pairId,
            double officialDelta,
            double javaDelta,
            boolean decided,
            boolean sameDirection,
            boolean strongInversion
    ) {
    }

    private record FeatureSummary(double mae, double p95, double maximum, String cause) {
    }

    private record Summary(
            Verdict verdict,
            int eligibleCaseCount,
            int diagnosticCaseCount,
            double mae,
            double p95,
            double cefrMatch,
            int decidedPairCount,
            double directionMatch,
            double spearman,
            long strongInversions
    ) {
    }

    private record CompatibilityReport(
            Summary summary,
            Map<String, FeatureSummary> featureSummaries,
            List<CaseResult> cases,
            List<PairResult> pairs,
            List<CaseResult> cefrCrossings
    ) {
    }

    private enum Verdict {
        ZIX_COMPATIBLE("ZIX-kompatibel"),
        TEXTBUDDY_PROXY_CANDIDATE("Textbuddy-Proxy-Kandidat"),
        INSUFFICIENT("Nicht ausreichend");

        private final String label;

        Verdict(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }
}
