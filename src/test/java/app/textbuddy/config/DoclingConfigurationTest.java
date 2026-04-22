package app.textbuddy.config;

import app.textbuddy.integration.docling.DoclingClient;
import app.textbuddy.integration.docling.HttpDoclingClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DoclingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DoclingConfiguration.class);

    @Test
    void failsFastWhenHttpModeIsMissingBaseUrl() {
        contextRunner
                .withPropertyValues("textbuddy.document.mode=http")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("textbuddy.document.base-url");
                });
    }

    @Test
    void startsWhenHttpModeHasBaseUrl() {
        contextRunner
                .withPropertyValues(
                        "textbuddy.document.mode=http",
                        "textbuddy.document.base-url=https://docling.example.test"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DoclingClient.class);
                    assertThat(context.getBean(DoclingClient.class)).isInstanceOf(HttpDoclingClient.class);
                });
    }
}
