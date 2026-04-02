package app.textbuddy.wordsynonym;

import app.textbuddy.integration.llm.WordSynonymLlmClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultWordSynonymServiceTest {

    @Test
    void mapsAndNormalizesLlmSynonyms() {
        WordSynonymLlmClient wordSynonymLlmClient = (word, context) -> List.of(
                "  rasch  ",
                "Rasch",
                "",
                "schnell",
                "flink",
                "zuegig"
        );
        DefaultWordSynonymService service = new DefaultWordSynonymService(wordSynonymLlmClient);

        WordSynonymResponse response = service.synonyms(
                new WordSynonymRequest("schnell", "Das ist schnell genug.")
        );

        assertThat(response.synonyms()).containsExactly(
                "rasch",
                "flink",
                "zuegig"
        );
    }

    @Test
    void skipsBlankWordsWithoutCallingLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        WordSynonymLlmClient wordSynonymLlmClient = (word, context) -> {
            called.set(true);
            return List.of();
        };
        DefaultWordSynonymService service = new DefaultWordSynonymService(wordSynonymLlmClient);

        WordSynonymResponse response = service.synonyms(
                new WordSynonymRequest("   ", "Kontext")
        );

        assertThat(called).isFalse();
        assertThat(response.synonyms()).isEmpty();
    }

    @Test
    void skipsMultipleWordsWithoutCallingLlm() {
        AtomicBoolean called = new AtomicBoolean(false);
        WordSynonymLlmClient wordSynonymLlmClient = (word, context) -> {
            called.set(true);
            return List.of();
        };
        DefaultWordSynonymService service = new DefaultWordSynonymService(wordSynonymLlmClient);

        WordSynonymResponse response = service.synonyms(
                new WordSynonymRequest("sehr gut", "Das ist sehr gut.")
        );

        assertThat(called).isFalse();
        assertThat(response.synonyms()).isEmpty();
    }
}
