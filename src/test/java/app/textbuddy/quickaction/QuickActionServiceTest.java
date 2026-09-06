package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.LlmProviderException;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QuickActionServiceTest {

    @ParameterizedTest
    @MethodSource("validActions")
    void executesEveryQuickAction(QuickActionType action, String option, String prompt) {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        QuickActionRequest request = new QuickActionRequest("Ausgangstext", "de-CH", option, prompt);
        MediumCurrentUser user = MediumCurrentUser.placeholder();
        when(llmClient.rewrite(eq(action), any(QuickActionRequest.class), eq(user))).thenReturn("Ergebnis");

        QuickActionResponse response = new QuickActionService(llmClient).execute(action, request, user);

        assertThat(response.text()).isEqualTo("Ergebnis");
        verify(llmClient).rewrite(eq(action), any(QuickActionRequest.class), eq(user));
    }

    @Test
    void skipsBlankText() {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);

        QuickActionResponse response = new QuickActionService(llmClient).execute(
                QuickActionType.PLAIN_LANGUAGE,
                new QuickActionRequest("  ", "de", null, null),
                MediumCurrentUser.placeholder()
        );

        assertThat(response.text()).isEmpty();
        verifyNoInteractions(llmClient);
    }

    @Test
    void preservesOuterWhitespaceInInputAndOutput() {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.rewrite(eq(QuickActionType.PLAIN_LANGUAGE), any(), any())).thenReturn("  Ergebnis  ");

        QuickActionResponse response = new QuickActionService(llmClient).execute(
                QuickActionType.PLAIN_LANGUAGE,
                new QuickActionRequest("  Ausgangstext  ", " de ", null, null),
                MediumCurrentUser.placeholder()
        );

        ArgumentCaptor<QuickActionRequest> request = ArgumentCaptor.forClass(QuickActionRequest.class);
        verify(llmClient).rewrite(eq(QuickActionType.PLAIN_LANGUAGE), request.capture(), any());
        assertThat(request.getValue().text()).isEqualTo("  Ausgangstext  ");
        assertThat(response.text()).isEqualTo("  Ergebnis  ");
    }

    @Test
    void rejectsBlankProviderOutput() {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.rewrite(eq(QuickActionType.PLAIN_LANGUAGE), any(), any())).thenReturn("  ");

        assertThatThrownBy(() -> new QuickActionService(llmClient).execute(
                QuickActionType.PLAIN_LANGUAGE,
                new QuickActionRequest("Ausgangstext", "de", null, null),
                MediumCurrentUser.placeholder()
        )).isInstanceOf(LlmProviderException.class);
    }

    private static Stream<Arguments> validActions() {
        return Stream.of(
                Arguments.of(QuickActionType.PLAIN_LANGUAGE, null, null),
                Arguments.of(QuickActionType.BULLET_POINTS, null, null),
                Arguments.of(QuickActionType.PROOFREAD, null, null),
                Arguments.of(QuickActionType.SUMMARIZE, "sentence", null),
                Arguments.of(QuickActionType.FORMALITY, "formal", null),
                Arguments.of(QuickActionType.SOCIAL_MEDIA, "linkedin", null),
                Arguments.of(QuickActionType.MEDIUM, "report", null),
                Arguments.of(QuickActionType.CHARACTER_SPEECH, "direct_speech", null),
                Arguments.of(QuickActionType.CUSTOM, null, "Kürzen")
        );
    }
}
