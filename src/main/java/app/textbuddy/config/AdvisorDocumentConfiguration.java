package app.textbuddy.config;

import app.textbuddy.advisor.AdvisorCatalogService;
import app.textbuddy.advisor.DefaultAdvisorCatalogService;
import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import app.textbuddy.integration.advisor.ClasspathAdvisorDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AdvisorDocumentConfiguration {

    @Bean
    AdvisorDocumentRepository advisorDocumentRepository(
            ApplicationContext applicationContext,
            ObjectMapper objectMapper
    ) {
        return new ClasspathAdvisorDocumentRepository(applicationContext, objectMapper);
    }

    @Bean
    AdvisorCatalogService advisorCatalogService(AdvisorDocumentRepository advisorDocumentRepository) {
        return new DefaultAdvisorCatalogService(advisorDocumentRepository);
    }
}
