package app.textbuddy.wordsynonym;

import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WordSynonymServiceTest {

    @Test
    void mapsAndNormalizesLlmSynonyms() {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        when(llmClient.suggestSynonyms("schnell", "Das ist schnell genug.")).thenReturn(List.of(
                "  rasch  ",
                "Rasch",
                "",
                "schnell",
                "flink",
                "zuegig"
        ));
        WordSynonymService service = new WordSynonymService(llmClient);

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
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        WordSynonymService service = new WordSynonymService(llmClient);

        WordSynonymResponse response = service.synonyms(
                new WordSynonymRequest("   ", "Kontext")
        );

        verifyNoInteractions(llmClient);
        assertThat(response.synonyms()).isEmpty();
    }

    @Test
    void skipsMultipleWordsWithoutCallingLlm() {
        TextbuddyLlmClient llmClient = mock(TextbuddyLlmClient.class);
        WordSynonymService service = new WordSynonymService(llmClient);

        WordSynonymResponse response = service.synonyms(
                new WordSynonymRequest("sehr gut", "Das ist sehr gut.")
        );

        verifyNoInteractions(llmClient);
        assertThat(response.synonyms()).isEmpty();
    }
}
