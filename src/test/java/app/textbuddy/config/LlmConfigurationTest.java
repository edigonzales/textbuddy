package app.textbuddy.config;

import app.textbuddy.integration.llm.OpenAiCompatibleChatClient;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class LlmConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JacksonAutoConfiguration.class, LlmConfiguration.class);

    @Test
    void failsFastWhenProviderModeIsMissingRequiredConfiguration() {
        contextRunner
                .withPropertyValues("textbuddy.llm.mode=provider")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("textbuddy.llm.base-url")
                            .hasMessageContaining("textbuddy.llm.api-key")
                            .hasMessageContaining("textbuddy.llm.model");
                });
    }

    @Test
    void startsWhenProviderModeHasRequiredConfiguration() {
        contextRunner
                .withPropertyValues(
                        "textbuddy.llm.mode=provider",
                        "textbuddy.llm.base-url=https://api.infomaniak.com/2/ai/103965/openai/v1/models",
                        "textbuddy.llm.api-key=test-token",
                        "textbuddy.llm.model=qwen3"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(TextbuddyLlmClient.class);
                    assertThat(context).hasSingleBean(OpenAiCompatibleChatClient.class);
                });
    }
}
