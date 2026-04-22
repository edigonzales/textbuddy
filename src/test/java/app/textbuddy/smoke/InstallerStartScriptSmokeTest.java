package app.textbuddy.smoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledOnOs({OS.LINUX, OS.MAC})
class InstallerStartScriptSmokeTest {

    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(20);

    @TempDir
    Path tempDir;

    @Test
    void startScriptRejectsJava17() throws Exception {
        Path releaseRoot = prepareReleaseLayout();
        Path fakeBin = Files.createDirectories(tempDir.resolve("fake-bin"));
        Path fakeJava = fakeBin.resolve("java");

        Files.writeString(fakeJava, """
                #!/usr/bin/env bash
                if [ "${1:-}" = "-version" ]; then
                  echo 'openjdk version "17.0.9"' >&2
                  exit 0
                fi
                exit 0
                """);
        Files.setPosixFilePermissions(fakeJava, executablePermissions());

        ProcessResult result = runScript(
                releaseRoot.resolve("bin/start-textbuddy.sh"),
                releaseRoot,
                fakeBin,
                ""
        );

        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.output()).contains("Java-Version ist zu alt");
    }

    @Test
    void startScriptPassesJvmAndAppArgumentsToJava() throws Exception {
        Path releaseRoot = prepareReleaseLayout();
        Path fakeBin = Files.createDirectories(tempDir.resolve("fake-bin"));
        Path fakeJava = fakeBin.resolve("java");
        Path argsFile = tempDir.resolve("java-args.log");

        Files.writeString(fakeJava, """
                #!/usr/bin/env bash
                if [ "${1:-}" = "-version" ]; then
                  echo 'openjdk version "25.0.1"' >&2
                  exit 0
                fi
                printf '%s\n' "$@" > "$FAKE_JAVA_ARGS_FILE"
                exit 0
                """);
        Files.setPosixFilePermissions(fakeJava, executablePermissions());

        ProcessResult result = runScript(
                releaseRoot.resolve("bin/start-textbuddy.sh"),
                releaseRoot,
                fakeBin,
                "-Xms64m -Xmx128m",
                "--server.port=19090",
                "--textbuddy.llm.mode=stub"
        );

        assertThat(result.exitCode()).isEqualTo(0);

        String loggedArgs = Files.readString(argsFile, StandardCharsets.UTF_8);
        assertThat(loggedArgs).contains("-Xms64m");
        assertThat(loggedArgs).contains("-Xmx128m");
        assertThat(loggedArgs).contains("-jar");
        assertThat(loggedArgs).contains(releaseRoot.resolve("textbuddy.jar").toString());
        assertThat(loggedArgs).contains("--server.port=19090");
        assertThat(loggedArgs).contains("--textbuddy.llm.mode=stub");
    }

    private Path prepareReleaseLayout() throws IOException {
        Path releaseRoot = Files.createDirectories(tempDir.resolve("release"));
        Path binDir = Files.createDirectories(releaseRoot.resolve("bin"));
        Path scriptSource = Path.of("distribution/bin/start-textbuddy.sh");
        Path scriptTarget = binDir.resolve("start-textbuddy.sh");

        Files.copy(scriptSource, scriptTarget, StandardCopyOption.REPLACE_EXISTING);
        Files.setPosixFilePermissions(scriptTarget, executablePermissions());
        Files.write(releaseRoot.resolve("textbuddy.jar"), new byte[0]);

        return releaseRoot;
    }

    private ProcessResult runScript(
            Path script,
            Path releaseRoot,
            Path fakeBin,
            String javaOpts,
            String... appArgs
    ) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command().add("bash");
        builder.command().add(script.toAbsolutePath().toString());

        for (String appArg : appArgs) {
            builder.command().add(appArg);
        }

        builder.directory(releaseRoot.toFile());
        builder.redirectErrorStream(true);
        builder.environment().put(
                "PATH",
                fakeBin.toAbsolutePath() + ":" + builder.environment().getOrDefault("PATH", "")
        );
        builder.environment().put("TEXTBUDDY_JAVA_OPTS", javaOpts);
        builder.environment().put(
                "FAKE_JAVA_ARGS_FILE",
                tempDir.resolve("java-args.log").toAbsolutePath().toString()
        );

        Process process = builder.start();
        String output = readAll(process.getInputStream());

        boolean finished = process.waitFor(PROCESS_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        return new ProcessResult(process.exitValue(), output);
    }

    private String readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        inputStream.transferTo(buffer);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private Set<PosixFilePermission> executablePermissions() {
        return Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
        );
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
