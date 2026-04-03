package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.FormalityPrompt;

import java.util.List;

public interface FormalityLlmClient {

    List<String> streamFormality(String text, String language, FormalityPrompt prompt);
}
