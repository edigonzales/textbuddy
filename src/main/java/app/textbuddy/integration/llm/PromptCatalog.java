package app.textbuddy.integration.llm;

import java.util.Map;

public interface PromptCatalog {

    String get(String key);

    String render(String key, Map<String, ?> variables);
}
