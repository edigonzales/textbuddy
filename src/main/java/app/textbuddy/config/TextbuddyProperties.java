package app.textbuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties(prefix = "textbuddy")
public class TextbuddyProperties {

    private final Auth auth = new Auth();
    private final Input input = new Input();
    private final Llm llm = new Llm();
    private final LanguageTool languagetool = new LanguageTool();
    private final Document document = new Document();

    public Auth getAuth() {
        return auth;
    }

    public Input getInput() {
        return input;
    }

    public Llm getLlm() {
        return llm;
    }

    public LanguageTool getLanguagetool() {
        return languagetool;
    }

    public Document getDocument() {
        return document;
    }

    public static class Auth {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Input {

        private int maxTextLength = 50_000;
        private int maxPromptLength = 2_000;

        public int getMaxTextLength() {
            return maxTextLength;
        }

        public void setMaxTextLength(int maxTextLength) {
            this.maxTextLength = Math.max(1, maxTextLength);
        }

        public int getMaxPromptLength() {
            return maxPromptLength;
        }

        public void setMaxPromptLength(int maxPromptLength) {
            this.maxPromptLength = Math.max(1, maxPromptLength);
        }
    }

    public static class Llm {

        public enum Mode {
            PROVIDER,
            STUB
        }

        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
        private Mode mode = Mode.PROVIDER;
        private String baseUrl = "";
        private String apiKey = "";
        private String model = "";
        private Duration timeout = DEFAULT_TIMEOUT;
        private double temperature = 0.2d;

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode == null ? Mode.PROVIDER : mode;
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

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = normalize(model);
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public boolean isStubMode() {
            return mode == Mode.STUB;
        }

        public String normalizedBaseUrl() {
            String value = normalize(baseUrl);
            if (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            if (value.endsWith("/models")) {
                value = value.substring(0, value.length() - "/models".length());
            }
            return value;
        }

        public URI normalizedBaseUri() {
            try {
                return URI.create(normalizedBaseUrl());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("textbuddy.llm.base-url ist keine gültige URI.", exception);
            }
        }

        public Duration normalizedTimeout() {
            return timeout == null || timeout.isZero() || timeout.isNegative() ? DEFAULT_TIMEOUT : timeout;
        }

        public double normalizedTemperature() {
            return Double.isFinite(temperature) ? Math.clamp(temperature, 0.0d, 2.0d) : 0.2d;
        }

        public void validateForProvider() {
            if (isStubMode()) {
                return;
            }
            List<String> missing = new ArrayList<>();
            if (normalizedBaseUrl().isBlank()) {
                missing.add("textbuddy.llm.base-url");
            }
            if (getApiKey().isBlank()) {
                missing.add("textbuddy.llm.api-key");
            }
            if (getModel().isBlank()) {
                missing.add("textbuddy.llm.model");
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException("LLM-Provider ist aktiv, aber folgende Properties fehlen: "
                        + String.join(", ", missing));
            }
            normalizedBaseUri();
        }
    }

    public static class LanguageTool {

        public enum Mode {
            EMBEDDED,
            HTTP,
            STUB
        }

        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
        private Mode mode = Mode.EMBEDDED;
        private String baseUrl = "";
        private String ngramPath = "";
        private Duration timeout = DEFAULT_TIMEOUT;

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

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
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
            String value = normalize(ngramPath);
            if (value.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(Path.of(value));
            } catch (InvalidPathException exception) {
                throw new IllegalStateException("textbuddy.languagetool.ngram-path ist kein gültiger Pfad.", exception);
            }
        }

        public Duration normalizedTimeout() {
            return timeout == null || timeout.isZero() || timeout.isNegative() ? DEFAULT_TIMEOUT : timeout;
        }

        public void validateForHttp() {
            if (isHttpMode() && normalizedBaseUrl().isBlank()) {
                throw new IllegalStateException(
                        "LanguageTool-HTTP-Modus ist aktiv, aber textbuddy.languagetool.base-url fehlt."
                );
            }
        }
    }

    public static class Document {

        public enum Mode {
            KREUZBERG,
            HTTP,
            STUB
        }

        private static final DataSize DEFAULT_MAX_UPLOAD_SIZE = DataSize.ofMegabytes(20);
        private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(45);
        private Mode mode = Mode.KREUZBERG;
        private String baseUrl = "";
        private String apiKey = "";
        private DataSize maxUploadSize = DEFAULT_MAX_UPLOAD_SIZE;
        private Duration timeout = DEFAULT_TIMEOUT;

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

        public DataSize getMaxUploadSize() {
            return maxUploadSize;
        }

        public void setMaxUploadSize(DataSize maxUploadSize) {
            this.maxUploadSize = maxUploadSize == null ? DEFAULT_MAX_UPLOAD_SIZE : maxUploadSize;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
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

        public long normalizedMaxUploadSizeBytes() {
            return maxUploadSize == null || maxUploadSize.toBytes() <= 0
                    ? DEFAULT_MAX_UPLOAD_SIZE.toBytes()
                    : maxUploadSize.toBytes();
        }
        public Duration normalizedTimeout() {
            return timeout == null || timeout.isZero() || timeout.isNegative() ? DEFAULT_TIMEOUT : timeout;
        }
        public long normalizedTimeoutSeconds() {
            return Math.max(1L, normalizedTimeout().toSeconds());
        }

        public String describeMaxUploadSize() {
            long mebibytes = normalizedMaxUploadSizeBytes() / (1024L * 1024L);
            return mebibytes <= 0 ? normalizedMaxUploadSizeBytes() + " Bytes" : mebibytes + " MB";
        }
        public void validateForHttp() {
            if (isHttpMode() && normalizedBaseUrl().isBlank()) {
                throw new IllegalStateException(
                        "Dokumentimport-HTTP-Modus ist aktiv, aber textbuddy.document.base-url fehlt."
                );
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
