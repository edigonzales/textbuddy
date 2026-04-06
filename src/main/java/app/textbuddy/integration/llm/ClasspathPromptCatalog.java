package app.textbuddy.integration.llm;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClasspathPromptCatalog implements PromptCatalog {

    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{\\{([a-zA-Z0-9_.-]+)}}");

    private final ResourceLoader resourceLoader;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public ClasspathPromptCatalog(ResourceLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader);
    }

    @Override
    public String get(String key) {
        return cache.computeIfAbsent(key, this::loadPrompt);
    }

    @Override
    public String render(String key, Map<String, ?> variables) {
        String rendered = get(key);

        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", Objects.toString(entry.getValue(), ""));
        }

        Matcher matcher = TEMPLATE_TOKEN.matcher(rendered);
        if (matcher.find()) {
            throw new IllegalArgumentException("Prompt-Template enthält unaufgelösten Platzhalter: " + matcher.group());
        }

        return rendered;
    }

    private String loadPrompt(String key) {
        Resource resource = resourceLoader.getResource("classpath:prompts/llm/" + key);

        if (!resource.exists()) {
            throw new IllegalArgumentException("Prompt-Ressource nicht gefunden: " + key);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Prompt-Ressource konnte nicht geladen werden: " + key, exception);
        }
    }
}
