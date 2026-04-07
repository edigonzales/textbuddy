package app.textbuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "textbuddy.document")
public class DocumentImportProperties {

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
        if (maxUploadSize == null || maxUploadSize.toBytes() <= 0) {
            return DEFAULT_MAX_UPLOAD_SIZE.toBytes();
        }

        return maxUploadSize.toBytes();
    }

    public Duration normalizedTimeout() {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return DEFAULT_TIMEOUT;
        }

        return timeout;
    }

    public long normalizedTimeoutSeconds() {
        long seconds = normalizedTimeout().toSeconds();
        return Math.max(1L, seconds);
    }

    public String describeMaxUploadSize() {
        long mebibytes = normalizedMaxUploadSizeBytes() / (1024L * 1024L);

        if (mebibytes <= 0) {
            return normalizedMaxUploadSizeBytes() + " Bytes";
        }

        return mebibytes + " MB";
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
