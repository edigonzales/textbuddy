package app.textbuddy.config;

import app.textbuddy.integration.languagetool.EmbeddedLanguageToolClient;
import app.textbuddy.integration.languagetool.LanguageToolClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LanguageToolConfiguration {

    @Bean
    LanguageToolClient languageToolClient(TextbuddyProperties properties) {
        return new EmbeddedLanguageToolClient(properties.getLanguagetool());
    }
}
