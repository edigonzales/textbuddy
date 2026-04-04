package app.textbuddy.config;

import app.textbuddy.integration.languagetool.HttpLanguageToolClient;
import app.textbuddy.integration.languagetool.LanguageToolClient;
import app.textbuddy.integration.languagetool.StubLanguageToolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class LanguageToolConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LanguageToolConfiguration.class);

    @Bean
    LanguageToolClient languageToolClient(@Value("${textbuddy.languagetool.base-url:}") String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.info("LanguageTool client: stub mode");
            return new StubLanguageToolClient();
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl.strip())
                .build();

        log.info("LanguageTool client: HTTP mode ({})", baseUrl.strip());
        return new HttpLanguageToolClient(restClient);
    }
}
