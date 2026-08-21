package app.textbuddy.sentencerewrite;

import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultSentenceRewriteServiceTest {

    @Test
    void mapsAndNormalizesLlmAlternatives() {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.rewriteSentence("Originalsatz.", "Der Satz steht in einem Absatz.")).thenReturn(List.of(
                "  Das klingt klarer.  ",
                "Das klingt klarer.",
                "",
                "Originalsatz.",
                "Praeziser formuliert."
        ));
        DefaultSentenceRewriteService service = new DefaultSentenceRewriteService(llmClient);

        SentenceRewriteResponse response = service.rewrite(new SentenceRewriteRequest(
                "Originalsatz.",
                "Der Satz steht in einem Absatz."
        ));

        assertThat(response.sentence()).isEqualTo("Originalsatz.");
        assertThat(response.options()).containsExactly(
                "Das klingt klarer.",
                "Praeziser formuliert."
        );
    }

    @Test
    void skipsBlankSentencesWithoutCallingLlm() {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        DefaultSentenceRewriteService service = new DefaultSentenceRewriteService(llmClient);

        SentenceRewriteResponse response = service.rewrite(new SentenceRewriteRequest("   ", "Kontext"));

        verifyNoInteractions(llmClient);
        assertThat(response.sentence()).isEqualTo("");
        assertThat(response.options()).isEmpty();
    }

    @Test
    void passesSentenceContextToTheLlmFacade() {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.rewriteSentence("Originalsatz.", "Absatz mit weiterem Kontext."))
                .thenReturn(List.of("Alternative."));
        DefaultSentenceRewriteService service = new DefaultSentenceRewriteService(llmClient);

        SentenceRewriteResponse response = service.rewrite(
                new SentenceRewriteRequest("Originalsatz.", "Absatz mit weiterem Kontext.")
        );

        assertThat(response.options()).containsExactly("Alternative.");
    }
}
