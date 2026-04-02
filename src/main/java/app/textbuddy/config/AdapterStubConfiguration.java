package app.textbuddy.config;

import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.languagetool.LanguageToolClient;
import app.textbuddy.integration.llm.LlmClientFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AdapterStubConfiguration {

    @Bean
    LanguageToolClient languageToolClient() {
        return new LanguageToolClient() {
        };
    }

    @Bean
    LlmClientFacade llmClientFacade() {
        return new LlmClientFacade() {
        };
    }

    @Bean
    DoclingClient doclingClient() {
        return new DoclingClient() {
        };
    }

    @Bean
    AdvisorDocumentRepository advisorDocumentRepository() {
        return new AdvisorDocumentRepository() {
        };
    }
}
