package app.textbuddy.config;

import app.textbuddy.integration.llm.OpenAiCompatibleChatClient;
import app.textbuddy.integration.llm.OpenAiTextbuddyLlmClient;
import app.textbuddy.integration.llm.PromptCatalog;
import app.textbuddy.integration.llm.LlmPrompts;
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
    TextbuddyLlmClient textbuddyLlmClient(
            ObjectMapper mapper,
            ResourceLoader resources,
            TextbuddyProperties textbuddyProperties
    ) {
        TextbuddyProperties.Llm properties = textbuddyProperties.getLlm();
        properties.validateForProvider();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.normalizedTimeout())
                .build();

        return new OpenAiTextbuddyLlmClient(
                new OpenAiCompatibleChatClient(httpClient, mapper, properties),
                new LlmPrompts(new PromptCatalog(resources), mapper)
        );
    }
}
