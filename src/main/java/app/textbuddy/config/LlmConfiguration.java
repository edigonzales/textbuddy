package app.textbuddy.config;

import app.textbuddy.integration.llm.AdvisorValidationLlmClient;
import app.textbuddy.integration.llm.BulletPointsLlmClient;
import app.textbuddy.integration.llm.CharacterSpeechLlmClient;
import app.textbuddy.integration.llm.ClasspathPromptCatalog;
import app.textbuddy.integration.llm.CustomLlmClient;
import app.textbuddy.integration.llm.FormalityLlmClient;
import app.textbuddy.integration.llm.LlmClientFacade;
import app.textbuddy.integration.llm.MediumLlmClient;
import app.textbuddy.integration.llm.OpenAiCompatibleChatClient;
import app.textbuddy.integration.llm.OpenAiQuickActionLlmClients;
import app.textbuddy.integration.llm.OpenAiStructuredLlmClients;
import app.textbuddy.integration.llm.PlainLanguageLlmClient;
import app.textbuddy.integration.llm.PromptCatalog;
import app.textbuddy.integration.llm.QuickActionPromptComposer;
import app.textbuddy.integration.llm.ProofreadLlmClient;
import app.textbuddy.integration.llm.SocialMediaLlmClient;
import app.textbuddy.integration.llm.StructuredPromptComposer;
import app.textbuddy.integration.llm.SummarizeLlmClient;
import app.textbuddy.integration.llm.WordSynonymLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
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
            LlmProperties properties
    ) {
        properties.validateForProvider();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.normalizedTimeout())
                .build();

        return new OpenAiCompatibleChatClient(httpClient, objectMapper, properties);
    }

    @Bean
    OpenAiQuickActionLlmClients openAiQuickActionLlmClients(
            OpenAiCompatibleChatClient chatClient,
            QuickActionPromptComposer quickActionPromptComposer
    ) {
        return new OpenAiQuickActionLlmClients(chatClient, quickActionPromptComposer);
    }

    @Bean
    OpenAiStructuredLlmClients openAiStructuredLlmClients(
            OpenAiCompatibleChatClient chatClient,
            StructuredPromptComposer structuredPromptComposer
    ) {
        return new OpenAiStructuredLlmClients(chatClient, structuredPromptComposer);
    }

    @Bean
    PlainLanguageLlmClient plainLanguageLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    BulletPointsLlmClient bulletPointsLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    ProofreadLlmClient proofreadLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    SummarizeLlmClient summarizeLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    FormalityLlmClient formalityLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    SocialMediaLlmClient socialMediaLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    MediumLlmClient mediumLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    CharacterSpeechLlmClient characterSpeechLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    CustomLlmClient customLlmClient(
            @Qualifier("openAiQuickActionLlmClients") OpenAiQuickActionLlmClients clients
    ) {
        return clients;
    }

    @Bean
    LlmClientFacade llmClientFacade(
            @Qualifier("openAiStructuredLlmClients") OpenAiStructuredLlmClients clients
    ) {
        return clients;
    }

    @Bean
    WordSynonymLlmClient wordSynonymLlmClient(
            @Qualifier("openAiStructuredLlmClients") OpenAiStructuredLlmClients clients
    ) {
        return clients;
    }

    @Bean
    AdvisorValidationLlmClient advisorValidationLlmClient(
            @Qualifier("openAiStructuredLlmClients") OpenAiStructuredLlmClients clients
    ) {
        return clients;
    }
}
