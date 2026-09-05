package app.textbuddy.web.i18n;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class UiMessageCatalogTest {

    private final UiMessageCatalog catalog = new UiMessageCatalog();

    @Test
    void exposesOnlyActuallyTranslatedLanguages() {
        assertThat(catalog.normalizeUiLocale(Locale.GERMAN)).isEqualTo(Locale.GERMAN);
        assertThat(catalog.normalizeUiLocale(Locale.ENGLISH)).isEqualTo(Locale.ENGLISH);
        assertThat(catalog.normalizeUiLocale(Locale.FRENCH)).isEqualTo(Locale.GERMAN);
    }

    @Test
    void englishBundleHasExactlyTheGermanKeys() throws IOException {
        assertThat(load("messages/ui_en.properties").stringPropertyNames())
                .isEqualTo(load("messages/ui.properties").stringPropertyNames());
    }

    private Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).as("classpath resource %s", resource).isNotNull();
            properties.load(input);
        }
        return properties;
    }
}
