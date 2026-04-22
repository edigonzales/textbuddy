package app.textbuddy;

import app.textbuddy.config.DocumentImportProperties;
import app.textbuddy.config.LanguageToolProperties;
import app.textbuddy.config.LlmProperties;
import app.textbuddy.config.TextbuddyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        TextbuddyProperties.class,
        LlmProperties.class,
        LanguageToolProperties.class,
        DocumentImportProperties.class
})
public class TextbuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TextbuddyApplication.class, args);
    }
}
