package app.textbuddy.config;

import app.textbuddy.integration.llm.StubTextbuddyLlmClient;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "textbuddy.llm", name = "mode", havingValue = "stub")
public class AdapterStubConfiguration {

    @Bean
    TextbuddyLlmClient textbuddyLlmClient() {
        return new StubTextbuddyLlmClient();
    }
}
