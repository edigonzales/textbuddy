package app.textbuddy.config;

import app.textbuddy.integration.languagetool.EmbeddedLanguageToolClient;
import app.textbuddy.integration.languagetool.HttpLanguageToolClient;
import app.textbuddy.integration.languagetool.LanguageToolClient;
import app.textbuddy.integration.languagetool.StubLanguageToolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LanguageToolProperties.class)
public class LanguageToolConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LanguageToolConfiguration.class);

    @Bean
    LanguageToolClient languageToolClient(LanguageToolProperties properties) {
        return switch (properties.getMode()) {
            case STUB -> {
                log.info("LanguageTool client: stub mode");
                yield new StubLanguageToolClient();
            }
            case HTTP -> {
                properties.validateForHttp();

                RestClient restClient = RestClient.builder()
                        .baseUrl(properties.normalizedBaseUrl())
                        .build();

                log.info("LanguageTool client: HTTP mode ({})", properties.normalizedBaseUrl());
                yield new HttpLanguageToolClient(restClient);
            }
            case EMBEDDED -> {
                log.info("LanguageTool client: embedded mode");
                yield new EmbeddedLanguageToolClient(properties);
            }
        };
    }
}
