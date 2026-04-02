package app.textbuddy.config;

import app.textbuddy.advisor.AdvisorCatalogService;
import app.textbuddy.advisor.AdvisorValidationService;
import app.textbuddy.document.DocumentConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoreStubConfiguration {

    @Bean
    AdvisorCatalogService advisorCatalogService() {
        return new AdvisorCatalogService() {
        };
    }

    @Bean
    AdvisorValidationService advisorValidationService() {
        return new AdvisorValidationService() {
        };
    }

    @Bean
    DocumentConversionService documentConversionService() {
        return new DocumentConversionService() {
        };
    }
}
