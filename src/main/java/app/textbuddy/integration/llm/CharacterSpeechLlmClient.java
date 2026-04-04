package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.CharacterSpeechPrompt;

import java.util.List;

public interface CharacterSpeechLlmClient {

    List<String> streamCharacterSpeech(String text, String language, CharacterSpeechPrompt prompt);
}
