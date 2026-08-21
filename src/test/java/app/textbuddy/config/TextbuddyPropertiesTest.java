package app.textbuddy.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextbuddyPropertiesTest {

    @Test
    void normalizesLlmConfiguration() {
        TextbuddyProperties.Llm properties = new TextbuddyProperties.Llm();
        properties.setBaseUrl("https://provider.example/v1/models");
        properties.setTimeout(Duration.ZERO);
        properties.setTemperature(99.0d);

        assertThat(properties.normalizedBaseUrl()).isEqualTo("https://provider.example/v1");
        assertThat(properties.normalizedTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.normalizedTemperature()).isEqualTo(2.0d);
    }

    @Test
    void validatesRequiredExternalAdapterUrls() {
        TextbuddyProperties.Llm llm = new TextbuddyProperties.Llm();
        TextbuddyProperties.LanguageTool languageTool = new TextbuddyProperties.LanguageTool();
        TextbuddyProperties.Document document = new TextbuddyProperties.Document();
        languageTool.setMode(TextbuddyProperties.LanguageTool.Mode.HTTP);
        document.setMode(TextbuddyProperties.Document.Mode.HTTP);

        assertThatThrownBy(llm::validateForProvider).hasMessageContaining("textbuddy.llm.base-url");
        assertThatThrownBy(languageTool::validateForHttp).hasMessageContaining("languagetool.base-url");
        assertThatThrownBy(document::validateForHttp).hasMessageContaining("document.base-url");
    }
}
