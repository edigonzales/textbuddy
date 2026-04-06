package app.textbuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

@ConfigurationProperties(prefix = "textbuddy.languagetool")
public class LanguageToolProperties {

    public enum Mode {
        EMBEDDED,
        HTTP,
        STUB
    }

    private Mode mode = Mode.EMBEDDED;
    private String baseUrl = "";
    private String ngramPath = "";

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.EMBEDDED : mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = normalize(baseUrl);
    }

    public String getNgramPath() {
        return ngramPath;
    }

    public void setNgramPath(String ngramPath) {
        this.ngramPath = normalize(ngramPath);
    }

    public boolean isEmbeddedMode() {
        return mode == Mode.EMBEDDED;
    }

    public boolean isHttpMode() {
        return mode == Mode.HTTP;
    }

    public boolean isStubMode() {
        return mode == Mode.STUB;
    }

    public String normalizedBaseUrl() {
        return normalize(baseUrl);
    }

    public Optional<Path> normalizedNgramPath() {
        String normalized = normalize(ngramPath);

        if (normalized.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Path.of(normalized));
        } catch (InvalidPathException exception) {
            throw new IllegalStateException("textbuddy.languagetool.ngram-path ist kein gültiger Pfad.", exception);
        }
    }

    public void validateForHttp() {
        if (!isHttpMode()) {
            return;
        }

        if (normalizedBaseUrl().isBlank()) {
            throw new IllegalStateException(
                    "LanguageTool-HTTP-Modus ist aktiv, aber textbuddy.languagetool.base-url fehlt."
            );
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
