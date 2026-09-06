package app.textbuddy.advisor;

import app.textbuddy.integration.llm.LlmProviderException;
import app.textbuddy.integration.llm.TextbuddyLlmClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdvisorServiceFixTest {

    @Test
    void fixValidatesCanonicalFindingAndPreservesOuterWhitespace() {
        AdvisorDocument document = document();
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document));
        TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
        when(client.fixAdvisor(anyString(), anyList())).thenReturn("Das gilt ab sofort.");
        AdvisorService service = new AdvisorService(catalog, client, 3);
        String original = "  Das gilt per sofort.  ";

        AdvisorFixResponse response = service.fix(new AdvisorFixRequest(original, List.of(
                new AdvisorFixRequest.Finding("schreibweisungen", "per-sofort-vermeiden", 11, 21, "ab sofort")
        )));

        assertThat(response.text()).isEqualTo("  Das gilt ab sofort.  ");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AdvisorFixFinding>> captor = ArgumentCaptor.forClass(List.class);
        verify(client).fixAdvisor(org.mockito.ArgumentMatchers.eq(original), captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(finding -> {
            assertThat(finding.matchedText()).isEqualTo("per sofort");
            assertThat(finding.instructions()).isEqualTo("Prüfe die Formulierung.");
            assertThat(finding.suggestion()).isEqualTo("ab sofort");
        });
    }

    @Test
    void fixRejectsEmptyUnknownDuplicateAndMismatchedFindings() {
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document()));
        AdvisorService service = new AdvisorService(catalog, mock(TextbuddyLlmClient.class), 3);
        String text = "Das gilt per sofort.";

        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of())));
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of(
                new AdvisorFixRequest.Finding("unbekannt", "regel", 9, 19, "ab sofort")
        ))));
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of(
                new AdvisorFixRequest.Finding("schreibweisungen", "per-sofort-vermeiden", 0, 3, "ab sofort")
        ))));
        AdvisorFixRequest.Finding valid = new AdvisorFixRequest.Finding(
                "schreibweisungen", "per-sofort-vermeiden", 9, 19, "ab sofort"
        );
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of(valid, valid))));
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, Collections.nCopies(21, valid))));
    }

    @Test
    void fixTreatsAnEmptyProviderResponseAsAnError() {
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document()));
        TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
        when(client.fixAdvisor(anyString(), anyList())).thenReturn("   ");
        AdvisorService service = new AdvisorService(catalog, client, 3);

        assertThatThrownBy(() -> service.fix(new AdvisorFixRequest("Das gilt per sofort.", List.of(
                new AdvisorFixRequest.Finding("schreibweisungen", "per-sofort-vermeiden", 9, 19, "ab sofort")
        )))).isInstanceOf(LlmProviderException.class);
    }

    @Test
    void validationLocatesRepeatedUnicodeTextWithoutTrimmingTheRequest() {
        AdvisorDocument document = document();
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.documents()).thenReturn(List.of(document));
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document));
        TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
        when(client.validate(anyString(), anyList())).thenReturn(List.of(
                new AdvisorRuleMatch("schreibweisungen", "per-sofort-vermeiden", "per sofort", "", "", ""),
                new AdvisorRuleMatch("schreibweisungen", "per-sofort-vermeiden", "per sofort", "", "", "")
        ));
        RecordingHandler handler = new RecordingHandler();
        String text = "  Ä: per sofort – später per sofort.  ";

        new AdvisorService(catalog, client, 3).validate(
                new AdvisorValidateRequest(text, List.of("schreibweisungen")), handler
        );

        verify(client).validate(org.mockito.ArgumentMatchers.eq(text), anyList());
        assertThat(handler.events).extracting(AdvisorValidationEvent::start).containsExactly(5, 25);
        assertThat(handler.events).extracting(AdvisorValidationEvent::end).containsExactly(15, 35);
        assertThat(handler.progress).containsExactly(new AdvisorProgressEvent(1, 1));
    }

    @Test
    void validationDiscardsProviderTextOutsideTheCanonicalMatchTerms() {
        AdvisorDocument document = document();
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.documents()).thenReturn(List.of(document));
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document));
        TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
        when(client.validate(anyString(), anyList())).thenReturn(List.of(
                new AdvisorRuleMatch("schreibweisungen", "per-sofort-vermeiden", "Das", "", "", "")
        ));
        RecordingHandler handler = new RecordingHandler();

        new AdvisorService(catalog, client, 3).validate(
                new AdvisorValidateRequest("Das gilt per sofort.", List.of("schreibweisungen")), handler
        );

        assertThat(handler.events).isEmpty();
        assertThat(handler.progress).containsExactly(new AdvisorProgressEvent(1, 1));
    }

    private void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(400));
    }

    private AdvisorDocument document() {
        return new AdvisorDocument(1, "schreibweisungen", "Schreibweisungen", "Demo", "Projektintern",
                "schreibweisungen.pdf", List.of(new AdvisorRule(
                "per-sofort-vermeiden", "Per sofort ersetzen", 1, "Prüfe die Formulierung.",
                "Die Formulierung ist intern.", "Nutze ab sofort.", List.of("per sofort")
        )));
    }

    private static final class RecordingHandler implements AdvisorValidationStreamHandler {
        private final java.util.ArrayList<AdvisorValidationEvent> events = new java.util.ArrayList<>();
        private final java.util.ArrayList<AdvisorProgressEvent> progress = new java.util.ArrayList<>();
        @Override public void validation(AdvisorValidationEvent event) { events.add(event); }
        @Override public void progress(AdvisorProgressEvent event) { progress.add(event); }
        @Override public void complete() { }
        @Override public void error(String message) { }
    }
}
