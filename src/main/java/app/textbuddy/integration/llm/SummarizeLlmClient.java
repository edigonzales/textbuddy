package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.SummarizePrompt;

import java.util.List;

public interface SummarizeLlmClient {

    List<String> streamSummarize(String text, String language, SummarizePrompt prompt);
}
