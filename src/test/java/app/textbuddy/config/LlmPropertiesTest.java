package app.textbuddy.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmPropertiesTest {

    @Test
    void normalizesBaseUrlByRemovingTrailingModelsEndpoint() {
        LlmProperties properties = new LlmProperties();
        properties.setBaseUrl("https://api.infomaniak.com/2/ai/103965/openai/v1/models");

        assertThat(properties.normalizedBaseUrl())
                .isEqualTo("https://api.infomaniak.com/2/ai/103965/openai/v1");
    }

    @Test
    void validatesRequiredProviderProperties() {
        LlmProperties properties = new LlmProperties();
        properties.setMode(LlmProperties.Mode.PROVIDER);

        assertThatThrownBy(properties::validateForProvider)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("textbuddy.llm.base-url")
                .hasMessageContaining("textbuddy.llm.api-key")
                .hasMessageContaining("textbuddy.llm.model");
    }

    @Test
    void normalizesTimeoutTemperatureAndRetries() {
        LlmProperties properties = new LlmProperties();
        properties.setTimeout(Duration.ZERO);
        properties.setTemperature(99.0d);
        properties.setMaxRetries(-5);

        assertThat(properties.normalizedTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.normalizedTemperature()).isEqualTo(2.0d);
        assertThat(properties.normalizedMaxRetries()).isZero();
    }
}
