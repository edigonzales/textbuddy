package app.textbuddy.integration.llm;

import java.util.List;

public interface WordSynonymLlmClient {
    List<String> suggestSynonyms(String word, String context);
}
