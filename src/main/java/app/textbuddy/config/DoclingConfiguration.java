package app.textbuddy.config;

import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.docling.HttpDoclingClient;
import app.textbuddy.integration.docling.KreuzbergDoclingClient;
import app.textbuddy.integration.docling.StubDoclingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DocumentImportProperties.class)
public class DoclingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DoclingConfiguration.class);

    @Bean
    DoclingClient doclingClient(DocumentImportProperties properties) {
        return switch (properties.getMode()) {
            case STUB -> {
                log.info("Document import: stub mode");
                yield new StubDoclingClient();
            }
            case HTTP -> {
                properties.validateForHttp();

                RestClient restClient = RestClient.builder()
                        .baseUrl(properties.normalizedBaseUrl())
                        .build();

                log.info("Document import: HTTP mode ({})", properties.normalizedBaseUrl());
                yield new HttpDoclingClient(restClient, properties.getApiKey());
            }
            case KREUZBERG -> {
                log.info("Document import: embedded Kreuzberg mode");
                yield new KreuzbergDoclingClient();
            }
        };
    }
}
