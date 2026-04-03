package app.textbuddy.integration.llm;

import java.util.List;

public interface ProofreadLlmClient {

    List<String> streamProofread(String text, String language);
}
