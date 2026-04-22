package app.textbuddy.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LanguageToolPropertiesTest {

    @Test
    void normalizesTimeoutAndRetries() {
        LanguageToolProperties properties = new LanguageToolProperties();
        properties.setTimeout(Duration.ZERO);
        properties.setMaxRetries(-3);

        assertThat(properties.normalizedTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.normalizedMaxRetries()).isZero();
    }

    @Test
    void validatesHttpModeRequiresBaseUrl() {
        LanguageToolProperties properties = new LanguageToolProperties();
        properties.setMode(LanguageToolProperties.Mode.HTTP);

        assertThatThrownBy(properties::validateForHttp)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("textbuddy.languagetool.base-url");
    }
}
