package app.textbuddy.config;

import app.textbuddy.integration.languagetool.HttpLanguageToolClient;
import app.textbuddy.integration.languagetool.LanguageToolClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageToolConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LanguageToolConfiguration.class);

    @Test
    void failsFastWhenHttpModeIsMissingBaseUrl() {
        contextRunner
                .withPropertyValues("textbuddy.languagetool.mode=http")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("textbuddy.languagetool.base-url");
                });
    }

    @Test
    void startsWhenHttpModeHasBaseUrl() {
        contextRunner
                .withPropertyValues(
                        "textbuddy.languagetool.mode=http",
                        "textbuddy.languagetool.base-url=https://languagetool.example.test"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(LanguageToolClient.class);
                    assertThat(context.getBean(LanguageToolClient.class)).isInstanceOf(HttpLanguageToolClient.class);
                });
    }
}
