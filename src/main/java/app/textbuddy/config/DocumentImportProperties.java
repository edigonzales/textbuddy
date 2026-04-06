package app.textbuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "textbuddy.document")
public class DocumentImportProperties {

    public enum Mode {
        KREUZBERG,
        HTTP,
        STUB
    }

    private Mode mode = Mode.KREUZBERG;
    private String baseUrl = "";
    private String apiKey = "";

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.KREUZBERG : mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = normalize(baseUrl);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = normalize(apiKey);
    }

    public boolean isKreuzbergMode() {
        return mode == Mode.KREUZBERG;
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

    public void validateForHttp() {
        if (!isHttpMode()) {
            return;
        }

        if (normalizedBaseUrl().isBlank()) {
            throw new IllegalStateException(
                    "Dokumentimport-HTTP-Modus ist aktiv, aber textbuddy.document.base-url fehlt."
            );
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
