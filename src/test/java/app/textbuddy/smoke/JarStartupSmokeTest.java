package app.textbuddy.smoke;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JarStartupSmokeTest {

    private static final Duration START_TIMEOUT = Duration.ofSeconds(60);

    @Test
    void bootJarStartsWithoutLanguageToolOrDoclingSidecars() throws Exception {
        String bootJarPath = System.getProperty("textbuddy.bootJarPath", "").trim();

        assertThat(bootJarPath)
                .as("bootJar path")
                .isNotBlank();
        assertThat(Path.of(bootJarPath))
                .as("bootJar file")
                .exists()
                .isRegularFile();

        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-jar",
                bootJarPath,
                "--server.port=0",
                "--textbuddy.auth.enabled=false",
                "--textbuddy.llm.mode=stub",
                "--textbuddy.languagetool.mode=embedded",
                "--textbuddy.document.mode=kreuzberg"
        )
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        Thread reader = startOutputReader(process, output);

        try {
            boolean started = waitForStartup(process, output);

            assertThat(started)
                    .withFailMessage("Jar ist nicht erfolgreich gestartet. Ausgabe:%n%s", output)
                    .isTrue();
        } finally {
            stopProcess(process);
            reader.join(10_000L);
        }
    }

    private Thread startOutputReader(Process process, StringBuilder output) {
        Thread reader = Thread.ofVirtual().start(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    synchronized (output) {
                        output.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException ignored) {
                synchronized (output) {
                    output.append("Ausgabe konnte nicht vollständig gelesen werden.").append(System.lineSeparator());
                }
            }
        });

        return reader;
    }

    private boolean waitForStartup(Process process, StringBuilder output) throws InterruptedException {
        long deadline = System.nanoTime() + START_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                return false;
            }

            String snapshot;

            synchronized (output) {
                snapshot = output.toString();
            }

            if (snapshot.contains("Started TextbuddyApplication")) {
                return true;
            }

            Thread.sleep(250L);
        }

        return false;
    }

    private void stopProcess(Process process) throws IOException, InterruptedException {
        if (!process.isAlive()) {
            return;
        }

        process.destroy();

        if (process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            return;
        }

        process.destroyForcibly();
        process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
    }
}
