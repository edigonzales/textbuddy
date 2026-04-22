package app.textbuddy.smoke;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JarEndToEndSmokeTest {

    @Test
    void coreFlowsWorkEndToEndAgainstPackagedJarWithoutSidecars() throws Exception {
        String bootJarPath = JarProcessHarness.requireBootJarPath();
        int port = JarProcessHarness.findFreePort();

        try (JarProcessHarness harness = JarProcessHarness.start(bootJarPath, port)) {
            HttpResponse<String> correction = harness.postJson("/api/text-correction", """
                    {
                      "text": "This is teh text.",
                      "language": "en-US"
                    }
                    """);
            assertThat(correction.statusCode()).isEqualTo(200);
            assertThat(correction.body()).contains("Spelling");

            HttpResponse<String> synonym = harness.postJson("/api/word-synonym", """
                    {
                      "word": "holprig",
                      "context": "Dieser Satz ist etwas holprig."
                    }
                    """);
            assertThat(synonym.statusCode()).isEqualTo(200);
            assertThat(synonym.body()).contains("hakelig");

            HttpResponse<String> rewrite = harness.postJson("/api/sentence-rewrite", """
                    {
                      "sentence": "Dieser Satz ist etwas holprig."
                    }
                    """);
            assertThat(rewrite.statusCode()).isEqualTo(200);
            assertThat(rewrite.body()).contains("Kurz gesagt");

            HttpResponse<String> stream = harness.postJson("/api/quick-actions/plain-language/stream", """
                    {
                      "text": "Der komplizierte Sachverhalt ist relevant.",
                      "language": "de-DE"
                    }
                    """);
            assertThat(stream.statusCode()).isEqualTo(200);
            assertThat(stream.body()).contains("event:complete");

            HttpResponse<String> advisorDocs = harness.get("/api/advisor/docs");
            assertThat(advisorDocs.statusCode()).isEqualTo(200);
            assertThat(advisorDocs.body()).contains("schreibweisungen");

            HttpResponse<String> converted = harness.postMultipart(
                    "/api/convert/doc?ocrLanguage=de",
                    "file",
                    "import.md",
                    "text/markdown",
                    "# Import Titel\n\nErste Zeile".getBytes(StandardCharsets.UTF_8)
            );
            assertThat(converted.statusCode()).isEqualTo(200);
            assertThat(converted.body()).contains("Import Titel");
            assertThat(converted.body()).contains("Erste Zeile");
        }
    }
}
