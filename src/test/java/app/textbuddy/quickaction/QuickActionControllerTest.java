package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.LlmProviderException;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.junit.jupiter.api.Test;
import app.textbuddy.config.TextbuddyProperties;
import app.textbuddy.web.RequestInputValidator;
import app.textbuddy.web.quickaction.QuickActionController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuickActionControllerTest {

    private final TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
    private final QuickActionController controller = new QuickActionController(
            client, new RequestInputValidator(new TextbuddyProperties())
    );

    @Test
    void forwardsEachActionAndPreservesTextWhitespace() {
        when(client.rewrite("  Ausgangstext  ", "de-CH", null)).thenReturn("  Ergebnis  ");
        when(client.rewrite("Ausgangstext", "de-CH", SummarizePrompt.SENTENCE)).thenReturn("Zusammenfassung");

        assertThat(controller.plainLanguage(new QuickActionRequest("  Ausgangstext  ", " de-CH ", null)).text())
                .isEqualTo("  Ergebnis  ");
        assertThat(controller.summarize(new QuickActionRequest("Ausgangstext", "de-CH", "sentence")).text())
                .isEqualTo("Zusammenfassung");
    }

    @Test
    void skipsBlankTextWithoutCallingTheProvider() {
        assertThat(controller.plainLanguage(new QuickActionRequest("  ", "de", null)).text()).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void rejectsBlankProviderOutput() {
        when(client.rewrite(anyString(), anyString(), isNull())).thenReturn("  ");
        assertThatThrownBy(() -> controller.plainLanguage(new QuickActionRequest("Ausgangstext", "de", null)))
                .isInstanceOf(LlmProviderException.class);
    }
}
