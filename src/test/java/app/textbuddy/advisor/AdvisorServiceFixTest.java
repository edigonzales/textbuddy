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
    void fixSendsOnlyVerifiedFindingsToTheProvider() {
        AdvisorDocument document = document();
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document));
        TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
        when(client.fixAdvisor(anyList())).thenReturn(List.of(""));
        AdvisorService service = new AdvisorService(catalog, client);
        String original = "  Das gilt per sofort.  ";

        List<String> response = service.fix(new AdvisorFixRequest(original, List.of(
                new AdvisorFixRequest.Finding("schreibweisungen", "per-sofort-vermeiden", 11, 21, "per sofort", " ")
        )));

        assertThat(response).containsExactly("");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AdvisorFixFinding>> captor = ArgumentCaptor.forClass(List.class);
        verify(client).fixAdvisor(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(finding -> {
            assertThat(finding.matchedText()).isEqualTo("per sofort");
            assertThat(finding.instructions()).isEqualTo("Prüfe die Formulierung.");
            assertThat(finding.suggestion()).isEqualTo(" ");
            assertThat(finding.context()).isEqualTo(original);
        });
    }

    @Test
    void fixRejectsEmptyUnknownDuplicateAndMismatchedFindings() {
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document()));
        AdvisorService service = new AdvisorService(catalog, mock(TextbuddyLlmClient.class));
        String text = "Das gilt per sofort.";

        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of())));
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of(
                new AdvisorFixRequest.Finding("unbekannt", "regel", 9, 19, "per sofort", "ab sofort")
        ))));
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of(
                new AdvisorFixRequest.Finding("schreibweisungen", "unbekannte-regel", 9, 19, "per sofort", "ab sofort")
        ))));
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of(
                new AdvisorFixRequest.Finding("schreibweisungen", "per-sofort-vermeiden", 0, 3, "per sofort", "ab sofort")
        ))));
        AdvisorFixRequest.Finding valid = new AdvisorFixRequest.Finding(
                "schreibweisungen", "per-sofort-vermeiden", 9, 19, "per sofort", "ab sofort"
        );
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, List.of(valid, valid))));
        assertBadRequest(() -> service.fix(new AdvisorFixRequest(text, Collections.nCopies(21, valid))));
    }

    @Test
    void fixTreatsAnEmptyProviderResponseAsAnError() {
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document()));
        TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
        when(client.fixAdvisor(anyList())).thenReturn(List.of());
        AdvisorService service = new AdvisorService(catalog, client);

        assertThatThrownBy(() -> service.fix(new AdvisorFixRequest("Das gilt per sofort.", List.of(
                new AdvisorFixRequest.Finding("schreibweisungen", "per-sofort-vermeiden", 9, 19, "per sofort", "ab sofort")
        )))).isInstanceOf(LlmProviderException.class);
    }

    @Test
    void validationUsesExactContextToLocateTheSecondOccurrence() {
        AdvisorDocument document = document();
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.documents()).thenReturn(List.of(document));
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document));
        TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
        when(client.validate(anyString(), anyList())).thenReturn(List.of(
                new AdvisorRuleMatch("schreibweisungen", "per-sofort-vermeiden", "per sofort", "", "", ""),
                new AdvisorRuleMatch("schreibweisungen", "per-sofort-vermeiden", "per sofort", "später per sofort.", "", "")
        ));
        RecordingHandler handler = new RecordingHandler();
        String text = "  🌱: per sofort – später per sofort.  ";

        new AdvisorService(catalog, client).validate(
                new AdvisorValidateRequest(text, List.of("schreibweisungen")), handler
        );

        verify(client).validate(org.mockito.ArgumentMatchers.eq(text), anyList());
        assertThat(handler.events).singleElement().satisfies(event -> {
            assertThat(event.start()).isEqualTo(text.lastIndexOf("per sofort"));
            assertThat(event.end()).isEqualTo(event.start() + "per sofort".length());
            assertThat(text.substring(event.start(), event.end())).isEqualTo("per sofort");
            assertThat(event.ruleId()).isEqualTo("per-sofort-vermeiden");
            assertThat(event.message()).isEqualTo("Die Formulierung ist intern.");
            assertThat(event.referenceUrl()).isEqualTo("/api/advisor/doc/schreibweisungen#page=1");
        });
        assertThat(handler.progress).containsExactly(new AdvisorProgressEvent(1, 1));
    }

    @Test
    void validationDiscardsUnknownRulesAndUnlocatableOrNoncanonicalText() {
        AdvisorDocument document = document();
        AdvisorCatalog catalog = mock(AdvisorCatalog.class);
        when(catalog.documents()).thenReturn(List.of(document));
        when(catalog.find("schreibweisungen")).thenReturn(Optional.of(document));
        TextbuddyLlmClient client = mock(TextbuddyLlmClient.class);
        when(client.validate(anyString(), anyList())).thenReturn(List.of(
                new AdvisorRuleMatch("schreibweisungen", "unbekannte-regel", "per sofort", "", "", ""),
                new AdvisorRuleMatch("schreibweisungen", "per-sofort-vermeiden", "Per sofort", "", "", ""),
                new AdvisorRuleMatch("schreibweisungen", "per-sofort-vermeiden", "Das", "", "", "")
        ));
        RecordingHandler handler = new RecordingHandler();

        new AdvisorService(catalog, client).validate(
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
        @Override public void complete(boolean truncated) { }
    }
}
