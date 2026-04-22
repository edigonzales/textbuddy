package app.textbuddy.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentImportPropertiesTest {

    @Test
    void normalizesTimeoutAndRetries() {
        DocumentImportProperties properties = new DocumentImportProperties();
        properties.setTimeout(Duration.ZERO);
        properties.setMaxRetries(-7);

        assertThat(properties.normalizedTimeout()).isEqualTo(Duration.ofSeconds(45));
        assertThat(properties.normalizedMaxRetries()).isZero();
    }

    @Test
    void validatesHttpModeRequiresBaseUrl() {
        DocumentImportProperties properties = new DocumentImportProperties();
        properties.setMode(DocumentImportProperties.Mode.HTTP);

        assertThatThrownBy(properties::validateForHttp)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("textbuddy.document.base-url");
    }
}
