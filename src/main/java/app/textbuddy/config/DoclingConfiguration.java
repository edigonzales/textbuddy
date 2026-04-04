package app.textbuddy.config;

import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.docling.HttpDoclingClient;
import app.textbuddy.integration.docling.StubDoclingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class DoclingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DoclingConfiguration.class);

    @Bean
    DoclingClient doclingClient(
            @Value("${textbuddy.docling.base-url:}") String baseUrl,
            @Value("${textbuddy.docling.api-key:}") String apiKey
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.info("Docling client: stub mode");
            return new StubDoclingClient();
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl.strip())
                .build();

        log.info("Docling client: HTTP mode ({})", baseUrl.strip());
        return new HttpDoclingClient(restClient, apiKey);
    }
}
