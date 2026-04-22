package app.textbuddy.web.i18n;

import app.textbuddy.config.WebI18nConfiguration;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Component
public class UiMessageCatalog {

    private static final List<String> SUPPORTED_UI_LANGUAGES = List.of("de", "en", "fr", "it");

    public Locale normalizeUiLocale(Locale requestedLocale) {
        String language = requestedLocale == null ? "" : requestedLocale.getLanguage();
        return SUPPORTED_UI_LANGUAGES.contains(language)
                ? Locale.forLanguageTag(language)
                : WebI18nConfiguration.DEFAULT_UI_LOCALE;
    }

    public List<String> supportedUiLanguages() {
        return SUPPORTED_UI_LANGUAGES;
    }

    public Map<String, String> resolve(Locale requestedLocale) {
        Locale locale = normalizeUiLocale(requestedLocale);
        ResourceBundle bundle = ResourceBundle.getBundle("messages.ui", locale);
        Map<String, String> messages = new LinkedHashMap<>();

        bundle.keySet().stream()
                .sorted()
                .forEach((key) -> messages.put(key, bundle.getString(key)));

        return messages;
    }
}
