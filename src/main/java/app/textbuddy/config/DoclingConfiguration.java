package app.textbuddy.config;

import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.docling.KreuzbergDoclingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DoclingConfiguration {

    @Bean
    DoclingClient doclingClient(TextbuddyProperties properties) {
        return new KreuzbergDoclingClient(properties.getDocument().normalizedTimeoutSeconds());
    }
}
