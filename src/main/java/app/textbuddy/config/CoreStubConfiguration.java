package app.textbuddy.config;

import app.textbuddy.advisor.AdvisorCatalogService;
import app.textbuddy.advisor.AdvisorValidationService;
import app.textbuddy.document.DocumentConversionService;
import app.textbuddy.quickaction.QuickActionService;
import app.textbuddy.sentencerewrite.SentenceRewriteService;
import app.textbuddy.wordsynonym.WordSynonymService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CoreStubConfiguration {

    @Bean
    SentenceRewriteService sentenceRewriteService() {
        return new SentenceRewriteService() {
        };
    }

    @Bean
    WordSynonymService wordSynonymService() {
        return new WordSynonymService() {
        };
    }

    @Bean
    QuickActionService quickActionService() {
        return new QuickActionService() {
        };
    }

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
