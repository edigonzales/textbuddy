package app.textbuddy.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeResourceInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    void createsRequiredRuntimeDirectories() {
        TextbuddyProperties.Runtime runtime = new TextbuddyProperties.Runtime();
        Path runtimeHome = tempDir.resolve("runtime-home");
        runtime.setHome(runtimeHome.toString());

        RuntimeResourceInitializer initializer = new RuntimeResourceInitializer();
        initializer.initialize(runtime);

        assertThat(runtime.requiredDirectories())
                .allMatch(Files::isDirectory)
                .allMatch(Files::isWritable);
    }

    @Test
    void failsWhenRuntimeHomeIsARegularFile() throws Exception {
        Path runtimeHomeFile = tempDir.resolve("runtime-home-file");
        Files.writeString(runtimeHomeFile, "not-a-directory");

        TextbuddyProperties.Runtime runtime = new TextbuddyProperties.Runtime();
        runtime.setHome(runtimeHomeFile.toString());

        RuntimeResourceInitializer initializer = new RuntimeResourceInitializer();

        assertThatThrownBy(() -> initializer.initialize(runtime))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kein Verzeichnis");
    }
}
