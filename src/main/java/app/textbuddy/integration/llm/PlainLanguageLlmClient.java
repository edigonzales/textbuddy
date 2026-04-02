package app.textbuddy.integration.llm;

import java.util.List;

public interface PlainLanguageLlmClient {

    List<String> streamPlainLanguage(String text, String language);
}
