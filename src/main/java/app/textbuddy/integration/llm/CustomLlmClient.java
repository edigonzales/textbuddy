package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.CustomQuickActionPrompt;

import java.util.List;

public interface CustomLlmClient {

    List<String> streamCustom(String text, String language, CustomQuickActionPrompt prompt);
}
