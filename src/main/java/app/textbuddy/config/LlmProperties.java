package app.textbuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "textbuddy.llm")
public class LlmProperties {

    public enum Mode {
        PROVIDER,
        STUB
    }

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final double DEFAULT_TEMPERATURE = 0.2d;

    private Mode mode = Mode.PROVIDER;
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private Duration timeout = DEFAULT_TIMEOUT;
    private double temperature = DEFAULT_TEMPERATURE;
    private int maxRetries = 1;

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

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
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
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return DEFAULT_TIMEOUT;
        }

        return timeout;
    }

    public double normalizedTemperature() {
        if (!Double.isFinite(temperature)) {
            return DEFAULT_TEMPERATURE;
        }

        return Math.clamp(temperature, 0.0d, 2.0d);
    }

    public int normalizedMaxRetries() {
        return Math.max(0, maxRetries);
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
            throw new IllegalStateException(
                    "LLM-Provider ist aktiv, aber folgende Properties fehlen: " + String.join(", ", missing)
            );
        }

        normalizedBaseUri();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
