package app.textbuddy.config;

import app.textbuddy.integration.advisor.AdvisorDocumentRepository;
import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.llm.LlmClientFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class AdapterStubConfiguration {

    @Bean
    LlmClientFacade llmClientFacade() {
        return sentence -> {
            String normalized = sentence == null ? "" : sentence.trim();

            if (normalized.isBlank()) {
                return List.of();
            }

            String punctuation = extractTrailingPunctuation(normalized);
            String stem = punctuation.isEmpty()
                    ? normalized
                    : normalized.substring(0, normalized.length() - punctuation.length()).trim();

            LinkedHashSet<String> alternatives = new LinkedHashSet<>();

            alternatives.add("Kurz gesagt: " + stem + punctuation);
            alternatives.add("Anders formuliert: " + stem + punctuation);
            alternatives.add("Praeziser gesagt: " + stem + punctuation);

            return List.copyOf(alternatives);
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

    private static String extractTrailingPunctuation(String sentence) {
        int index = sentence.length();

        while (index > 0) {
            char current = sentence.charAt(index - 1);

            if (current != '.' && current != '!' && current != '?') {
                break;
            }

            index -= 1;
        }

        return sentence.substring(index);
    }
}
