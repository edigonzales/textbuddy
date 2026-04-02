package app.textbuddy.sentencerewrite;

import app.textbuddy.integration.llm.LlmClientFacade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSentenceRewriteServiceTest {

    @Test
    void mapsAndNormalizesLlmAlternatives() {
        LlmClientFacade llmClientFacade = sentence -> List.of(
                "  Das klingt klarer.  ",
                "Das klingt klarer.",
                "",
                "Originalsatz.",
                "Praeziser formuliert."
        );
        DefaultSentenceRewriteService service = new DefaultSentenceRewriteService(llmClientFacade);

        SentenceRewriteResponse response = service.rewrite(new SentenceRewriteRequest("Originalsatz."));

        assertThat(response.original()).isEqualTo("Originalsatz.");
        assertThat(response.alternatives()).containsExactly(
                "Das klingt klarer.",
                "Praeziser formuliert."
        );
    }

    @Test
    void skipsBlankSentencesWithoutCallingLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        LlmClientFacade llmClientFacade = sentence -> {
            called.set(true);
            return List.of();
        };
        DefaultSentenceRewriteService service = new DefaultSentenceRewriteService(llmClientFacade);

        SentenceRewriteResponse response = service.rewrite(new SentenceRewriteRequest("   "));

        assertThat(called).isFalse();
        assertThat(response.original()).isEqualTo("");
        assertThat(response.alternatives()).isEmpty();
    }
}
