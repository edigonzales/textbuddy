package app.textbuddy.integration.llm;

import java.util.List;

public interface LlmClientFacade {
    List<String> rewriteSentence(String sentence);
}
