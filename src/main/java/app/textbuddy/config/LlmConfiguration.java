package app.textbuddy.config;

import app.textbuddy.integration.llm.ClasspathPromptCatalog;
import app.textbuddy.integration.llm.OpenAiCompatibleChatClient;
import app.textbuddy.integration.llm.OpenAiTextbuddyLlmClient;
import app.textbuddy.integration.llm.PromptCatalog;
import app.textbuddy.integration.llm.QuickActionPromptComposer;
import app.textbuddy.integration.llm.StructuredPromptComposer;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TextbuddyProperties.class)
@ConditionalOnProperty(prefix = "textbuddy.llm", name = "mode", havingValue = "provider", matchIfMissing = true)
public class LlmConfiguration {

    @Bean
    PromptCatalog promptCatalog(ResourceLoader resourceLoader) {
        return new ClasspathPromptCatalog(resourceLoader);
    }

    @Bean
    QuickActionPromptComposer quickActionPromptComposer(PromptCatalog promptCatalog) {
        return new QuickActionPromptComposer(promptCatalog);
    }

    @Bean
    StructuredPromptComposer structuredPromptComposer(
            PromptCatalog promptCatalog,
            ObjectMapper objectMapper
    ) {
        return new StructuredPromptComposer(promptCatalog, objectMapper);
    }

    @Bean
    OpenAiCompatibleChatClient openAiCompatibleChatClient(
            ObjectMapper objectMapper,
            TextbuddyProperties textbuddyProperties
    ) {
        TextbuddyProperties.Llm properties = textbuddyProperties.getLlm();
        properties.validateForProvider();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.normalizedTimeout())
                .build();

        return new OpenAiCompatibleChatClient(httpClient, objectMapper, properties);
    }

    @Bean
    TextbuddyLlmClient textbuddyLlmClient(
            OpenAiCompatibleChatClient chatClient,
            QuickActionPromptComposer quickActionPromptComposer,
            StructuredPromptComposer structuredPromptComposer
    ) {
        return new OpenAiTextbuddyLlmClient(chatClient, quickActionPromptComposer, structuredPromptComposer);
    }
}
