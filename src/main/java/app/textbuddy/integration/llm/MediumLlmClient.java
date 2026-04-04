package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.MediumPrompt;

import java.util.List;

public interface MediumLlmClient {

    List<String> streamMedium(String text, String language, MediumPrompt prompt);
}
