package app.textbuddy.config;

import app.textbuddy.document.DocumentImportFormatCatalog;
import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class OperationsConfiguration {

    @Bean
    RuntimeResourceInitializer runtimeResourceInitializer() {
        return new RuntimeResourceInitializer();
    }

    @Bean
    SmartInitializingSingleton runtimeResourceProvisioningInitializer(
            TextbuddyProperties textbuddyProperties,
            RuntimeResourceInitializer runtimeResourceInitializer
    ) {
        return () -> {
            if (textbuddyProperties.getRuntime().isInitializeLocalResources()) {
                runtimeResourceInitializer.initialize(textbuddyProperties.getRuntime());
            }
        };
    }

    @Bean
    SmartInitializingSingleton startupConfigurationValidator(
            LlmProperties llmProperties,
            LanguageToolProperties languageToolProperties,
            DocumentImportProperties documentImportProperties,
            AdvisorDocumentRepository advisorDocumentRepository
    ) {
        return () -> {
            llmProperties.validateForProvider();
            languageToolProperties.validateForHttp();
            documentImportProperties.validateForHttp();
            advisorDocumentRepository.findAll();
        };
    }

    @Bean
    InfoContributor textbuddyInfoContributor(
            TextbuddyProperties textbuddyProperties,
            LlmProperties llmProperties,
            LanguageToolProperties languageToolProperties,
            DocumentImportProperties documentImportProperties
    ) {
        return (builder) -> builder.withDetail("textbuddy", Map.of(
                "authEnabled", textbuddyProperties.getAuth().isEnabled(),
                "llmMode", llmProperties.getMode().name().toLowerCase(),
                "languageToolMode", languageToolProperties.getMode().name().toLowerCase(),
                "documentImportMode", documentImportProperties.getMode().name().toLowerCase(),
                "runtimeHome", textbuddyProperties.getRuntime().normalizedHomePath().toString(),
                "runtimeInitializationEnabled", textbuddyProperties.getRuntime().isInitializeLocalResources()
        ));
    }

    @Bean
    HealthIndicator llmHealthIndicator(LlmProperties properties) {
        return () -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("mode", properties.getMode().name().toLowerCase());

            if (!properties.isStubMode()) {
                details.put("baseUrl", properties.normalizedBaseUrl());
                details.put("model", properties.getModel());
                details.put("timeoutMs", properties.normalizedTimeout().toMillis());
                details.put("maxRetries", properties.normalizedMaxRetries());
            }

            try {
                properties.validateForProvider();
                return Health.up().withDetails(details).build();
            } catch (RuntimeException exception) {
                return Health.down(exception).withDetails(details).build();
            }
        };
    }

    @Bean
    HealthIndicator languagetoolHealthIndicator(LanguageToolProperties properties) {
        return () -> {
            try {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("mode", properties.getMode().name().toLowerCase());
                details.put("timeoutMs", properties.normalizedTimeout().toMillis());
                details.put("maxRetries", properties.normalizedMaxRetries());

                if (properties.isHttpMode()) {
                    details.put("baseUrl", properties.normalizedBaseUrl());
                }

                if (properties.normalizedNgramPath().isPresent()) {
                    details.put("ngramPath", properties.normalizedNgramPath().get().toString());
                }

                properties.validateForHttp();

                if (properties.normalizedNgramPath().isPresent()
                        && !Files.isDirectory(properties.normalizedNgramPath().get())) {
                    return Health.down()
                            .withDetails(details)
                            .withDetail("error", "textbuddy.languagetool.ngram-path muss auf ein Verzeichnis zeigen.")
                            .build();
                }

                return Health.up().withDetails(details).build();
            } catch (RuntimeException exception) {
                return Health.down(exception)
                        .withDetail("mode", properties.getMode().name().toLowerCase())
                        .build();
            }
        };
    }

    @Bean
    HealthIndicator documentImportHealthIndicator(
            DocumentImportProperties properties,
            DocumentImportFormatCatalog formatCatalog
    ) {
        return () -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("mode", properties.getMode().name().toLowerCase());
            details.put("timeoutMs", properties.normalizedTimeout().toMillis());
            details.put("maxRetries", properties.normalizedMaxRetries());
            details.put("maxUploadBytes", properties.normalizedMaxUploadSizeBytes());
            details.put("supportedFormats", formatCatalog.listFormats().size());

            if (properties.isHttpMode()) {
                details.put("baseUrl", properties.normalizedBaseUrl());
            }

            try {
                properties.validateForHttp();

                if (formatCatalog.listFormats().isEmpty()) {
                    return Health.down()
                            .withDetails(details)
                            .withDetail("error", "Kein unterstütztes Dokumentformat verfügbar.")
                            .build();
                }

                return Health.up().withDetails(details).build();
            } catch (RuntimeException exception) {
                return Health.down(exception).withDetails(details).build();
            }
        };
    }
}
