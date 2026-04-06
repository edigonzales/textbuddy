package app.textbuddy.integration.llm;

import app.textbuddy.quickaction.CharacterSpeechPrompt;
import app.textbuddy.quickaction.CustomQuickActionPrompt;
import app.textbuddy.quickaction.FormalityPrompt;
import app.textbuddy.quickaction.MediumCurrentUser;
import app.textbuddy.quickaction.MediumPrompt;
import app.textbuddy.quickaction.SocialMediaPrompt;
import app.textbuddy.quickaction.SummarizePrompt;

import java.util.List;
import java.util.Objects;

public final class OpenAiQuickActionLlmClients implements
        PlainLanguageLlmClient,
        BulletPointsLlmClient,
        ProofreadLlmClient,
        SummarizeLlmClient,
        FormalityLlmClient,
        SocialMediaLlmClient,
        MediumLlmClient,
        CharacterSpeechLlmClient,
        CustomLlmClient {

    private final OpenAiCompatibleChatClient chatClient;
    private final QuickActionPromptComposer promptComposer;

    public OpenAiQuickActionLlmClients(
            OpenAiCompatibleChatClient chatClient,
            QuickActionPromptComposer promptComposer
    ) {
        this.chatClient = Objects.requireNonNull(chatClient);
        this.promptComposer = Objects.requireNonNull(promptComposer);
    }

    @Override
    public List<String> streamPlainLanguage(String text, String language) {
        return stream(promptComposer.plainLanguage(text, language));
    }

    @Override
    public List<String> streamBulletPoints(String text, String language) {
        return stream(promptComposer.bulletPoints(text, language));
    }

    @Override
    public List<String> streamProofread(String text, String language) {
        return stream(promptComposer.proofread(text, language));
    }

    @Override
    public List<String> streamSummarize(String text, String language, SummarizePrompt prompt) {
        return stream(promptComposer.summarize(text, language, prompt));
    }

    @Override
    public List<String> streamFormality(String text, String language, FormalityPrompt prompt) {
        return stream(promptComposer.formality(text, language, prompt));
    }

    @Override
    public List<String> streamSocialMedia(String text, String language, SocialMediaPrompt prompt) {
        return stream(promptComposer.socialMedia(text, language, prompt));
    }

    @Override
    public List<String> streamMedium(String text, String language, MediumPrompt prompt, MediumCurrentUser currentUser) {
        return stream(promptComposer.medium(text, language, prompt, currentUser));
    }

    @Override
    public List<String> streamCharacterSpeech(String text, String language, CharacterSpeechPrompt prompt) {
        return stream(promptComposer.characterSpeech(text, language, prompt));
    }

    @Override
    public List<String> streamCustom(String text, String language, CustomQuickActionPrompt prompt) {
        return stream(promptComposer.custom(text, language, prompt));
    }

    private List<String> stream(PromptMessages promptMessages) {
        return chatClient.streamText(promptMessages.systemPrompt(), promptMessages.userPrompt());
    }
}
