package app.textbuddy.config;

import app.textbuddy.integration.languagetool.HttpLanguageToolClient;
import app.textbuddy.integration.languagetool.LanguageToolClient;
import app.textbuddy.integration.languagetool.StubLanguageToolClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class LanguageToolConfiguration {

    @Bean
    LanguageToolClient languageToolClient(@Value("${textbuddy.languagetool.base-url:}") String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return new StubLanguageToolClient();
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl.strip())
                .build();

        return new HttpLanguageToolClient(restClient);
    }
}
