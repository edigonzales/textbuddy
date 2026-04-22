package app.textbuddy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RuntimeResourceInitializer {

    private static final Logger log = LoggerFactory.getLogger(RuntimeResourceInitializer.class);

    public void initialize(TextbuddyProperties.Runtime runtimeProperties) {
        for (Path directory : runtimeProperties.requiredDirectories()) {
            ensureDirectory(directory);
        }

        log.info("Runtime resources initialized at {}", runtimeProperties.normalizedHomePath());
    }

    private void ensureDirectory(Path directory) {
        try {
            if (Files.exists(directory) && !Files.isDirectory(directory)) {
                throw new IllegalStateException("Lokaler Laufzeitpfad ist kein Verzeichnis: " + directory);
            }

            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Lokale Laufzeitressourcen konnten nicht initialisiert werden: " + directory,
                    exception
            );
        }

        if (!Files.isWritable(directory)) {
            throw new IllegalStateException("Lokaler Laufzeitpfad ist nicht beschreibbar: " + directory);
        }
    }
}
