package app.textbuddy.sentencerewrite;

import app.textbuddy.integration.llm.LlmClientFacade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSentenceRewriteServiceTest {

    @Test
    void mapsAndNormalizesLlmAlternatives() {
        LlmClientFacade llmClientFacade = (sentence, context) -> List.of(
                "  Das klingt klarer.  ",
                "Das klingt klarer.",
                "",
                "Originalsatz.",
                "Praeziser formuliert."
        );
        DefaultSentenceRewriteService service = new DefaultSentenceRewriteService(llmClientFacade);

        SentenceRewriteResponse response = service.rewrite(new SentenceRewriteRequest(
                "Originalsatz.",
                "Der Satz steht in einem Absatz."
        ));

        assertThat(response.original()).isEqualTo("Originalsatz.");
        assertThat(response.alternatives()).containsExactly(
                "Das klingt klarer.",
                "Praeziser formuliert."
        );
    }

    @Test
    void skipsBlankSentencesWithoutCallingLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        LlmClientFacade llmClientFacade = (sentence, context) -> {
            called.set(true);
            return List.of();
        };
        DefaultSentenceRewriteService service = new DefaultSentenceRewriteService(llmClientFacade);

        SentenceRewriteResponse response = service.rewrite(new SentenceRewriteRequest("   ", "Kontext"));

        assertThat(called).isFalse();
        assertThat(response.original()).isEqualTo("");
        assertThat(response.alternatives()).isEmpty();
    }

    @Test
    void passesSentenceContextToTheLlmFacade() {
        AtomicBoolean called = new AtomicBoolean(false);
        LlmClientFacade llmClientFacade = (sentence, context) -> {
            called.set(true);
            assertThat(sentence).isEqualTo("Originalsatz.");
            assertThat(context).isEqualTo("Absatz mit weiterem Kontext.");
            return List.of("Alternative.");
        };
        DefaultSentenceRewriteService service = new DefaultSentenceRewriteService(llmClientFacade);

        SentenceRewriteResponse response = service.rewrite(
                new SentenceRewriteRequest("Originalsatz.", "Absatz mit weiterem Kontext.")
        );

        assertThat(called).isTrue();
        assertThat(response.alternatives()).containsExactly("Alternative.");
    }
}
