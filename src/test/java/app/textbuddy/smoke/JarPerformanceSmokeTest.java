package app.textbuddy.smoke;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JarPerformanceSmokeTest {

    private static final long CORRECTION_LIMIT_MS = 12_000L;
    private static final long REWRITE_LIMIT_MS = 8_000L;
    private static final long ADVISOR_LIMIT_MS = 8_000L;
    private static final long IMPORT_LIMIT_MS = 12_000L;

    @Test
    void performanceSmokeForCorrectionRewriteAdvisorAndImport() throws Exception {
        String bootJarPath = JarProcessHarness.requireBootJarPath();
        int port = JarProcessHarness.findFreePort();

        try (JarProcessHarness harness = JarProcessHarness.start(bootJarPath, port)) {
            MeasuredResponse correction = measure(() -> harness.postJson("/api/text-correction", """
                    {
                      "text": "This is teh text with recieve and  extra spaces.",
                      "language": "en-US"
                    }
                    """));
            assertThat(correction.response().statusCode()).isEqualTo(200);
            assertThat(correction.durationMs()).isLessThan(CORRECTION_LIMIT_MS);

            MeasuredResponse rewrite = measure(() -> harness.postJson("/api/sentence-rewrite", """
                    {
                      "sentence": "Dieser Satz ist etwas holprig."
                    }
                    """));
            assertThat(rewrite.response().statusCode()).isEqualTo(200);
            assertThat(rewrite.durationMs()).isLessThan(REWRITE_LIMIT_MS);

            MeasuredResponse advisor = measure(() -> harness.postJson("/api/advisor/validate", """
                    {
                      "text": "Diese Regel gilt per sofort für alle Schreiben.",
                      "docs": ["schreibweisungen"]
                    }
                    """));
            assertThat(advisor.response().statusCode()).isEqualTo(200);
            assertThat(advisor.response().body()).contains("event:validation");
            assertThat(advisor.durationMs()).isLessThan(ADVISOR_LIMIT_MS);

            MeasuredResponse documentImport = measure(() -> harness.postMultipart(
                    "/api/convert/doc?ocrLanguage=de",
                    "file",
                    "performance-import.md",
                    "text/markdown",
                    "# Import\n\nDer Inhalt ist kompakt und testbar.".getBytes(StandardCharsets.UTF_8)
            ));
            assertThat(documentImport.response().statusCode()).isEqualTo(200);
            assertThat(documentImport.durationMs()).isLessThan(IMPORT_LIMIT_MS);
        }
    }

    private MeasuredResponse measure(CheckedRequest request) throws Exception {
        long startNanos = System.nanoTime();
        HttpResponse<String> response = request.execute();
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        return new MeasuredResponse(response, durationMs);
    }

    @FunctionalInterface
    private interface CheckedRequest {
        HttpResponse<String> execute() throws Exception;
    }

    private record MeasuredResponse(HttpResponse<String> response, long durationMs) {
    }
}
