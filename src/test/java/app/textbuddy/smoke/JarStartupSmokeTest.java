package app.textbuddy.smoke;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JarStartupSmokeTest {

    @Test
    void packagedJarStartsAsTextbuddyJarWithoutExternalSidecars() throws Exception {
        String bootJarPath = JarProcessHarness.requireBootJarPath();

        assertThat(Path.of(bootJarPath).getFileName().toString())
                .isEqualTo("textbuddy.jar");

        int port = JarProcessHarness.findFreePort();

        try (JarProcessHarness harness = JarProcessHarness.start(bootJarPath, port)) {
            HttpResponse<String> health = harness.get("/actuator/health");
            HttpResponse<String> info = harness.get("/actuator/info");

            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(health.body()).contains("\"status\":\"UP\"");

            assertThat(info.statusCode()).isEqualTo(200);
            assertThat(info.body()).contains("\"llmMode\":\"stub\"");
            assertThat(info.body()).contains("\"languageToolMode\":\"embedded\"");
            assertThat(info.body()).contains("\"documentImportMode\":\"kreuzberg\"");
        }
    }

    @Test
    void jarCanShutdownAndRecoverOnSamePort() throws Exception {
        String bootJarPath = JarProcessHarness.requireBootJarPath();
        int port = JarProcessHarness.findFreePort();

        try (JarProcessHarness firstRun = JarProcessHarness.start(bootJarPath, port)) {
            HttpResponse<String> health = firstRun.get("/actuator/health");
            assertThat(health.statusCode()).isEqualTo(200);
        }

        try (JarProcessHarness secondRun = JarProcessHarness.start(bootJarPath, port)) {
            HttpResponse<String> health = secondRun.get("/actuator/health");
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(secondRun.isAlive()).isTrue();
        }
    }
}
