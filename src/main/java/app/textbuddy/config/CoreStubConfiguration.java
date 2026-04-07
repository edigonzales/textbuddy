package app.textbuddy.config;

import app.textbuddy.advisor.AdvisorValidationService;
import app.textbuddy.document.DocumentConversionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoreStubConfiguration {

    @Bean
    @ConditionalOnMissingBean(DocumentConversionService.class)
    DocumentConversionService documentConversionService() {
        return (upload, ocrLanguage) -> {
            throw new UnsupportedOperationException("DocumentConversionService ist nicht konfiguriert.");
        };
    }
}
