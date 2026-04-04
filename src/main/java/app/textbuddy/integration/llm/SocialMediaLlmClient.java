package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.SocialMediaPrompt;

import java.util.List;

public interface SocialMediaLlmClient {

    List<String> streamSocialMedia(String text, String language, SocialMediaPrompt prompt);
}
