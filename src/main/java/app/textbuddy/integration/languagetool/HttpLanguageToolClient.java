package app.textbuddy.integration.languagetool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

public final class HttpLanguageToolClient implements LanguageToolClient {

    private final RestClient restClient;

    public HttpLanguageToolClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<LanguageToolMatch> check(String text, String language) {
        return checkOnce(text, language);
    }

    private List<LanguageToolMatch> checkOnce(String text, String language) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("text", text);
        form.add("language", language);

        LanguageToolCheckResponse response;

        try {
            response = restClient.post()
                    .uri("/v2/check")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(LanguageToolCheckResponse.class);
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure(exception);
        } catch (ResourceAccessException exception) {
            throw new LanguageToolUnavailableException(
                    "LanguageTool ist momentan nicht erreichbar.",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new LanguageToolUnavailableException(
                    "LanguageTool-Aufruf ist fehlgeschlagen.",
                    exception
            );
        }

        if (response == null || response.matches() == null) {
            return List.of();
        }

        return response.matches().stream()
                .map(this::mapMatch)
                .toList();
    }

    private RuntimeException mapHttpFailure(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        String message = switch (statusCode) {
            case 401, 403 -> "LanguageTool lehnt die Anmeldedaten ab.";
            case 429 -> "LanguageTool hat das Rate Limit erreicht.";
            default -> statusCode >= 500
                    ? "LanguageTool ist momentan nicht verfügbar."
                    : "LanguageTool antwortete mit HTTP " + statusCode + ".";
        };

        return new LanguageToolUnavailableException(message);
    }

    private LanguageToolMatch mapMatch(LanguageToolMatchResponse match) {
        List<String> replacements = match.replacements() == null
                ? List.of()
                : match.replacements().stream()
                .map(LanguageToolReplacementResponse::value)
                .toList();

        return new LanguageToolMatch(
                match.offset(),
                match.length(),
                match.message(),
                match.shortMessage(),
                match.rule() == null ? "" : match.rule().id(),
                replacements
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LanguageToolCheckResponse(List<LanguageToolMatchResponse> matches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LanguageToolMatchResponse(
            int offset,
            int length,
            String message,
            String shortMessage,
            LanguageToolRuleResponse rule,
            List<LanguageToolReplacementResponse> replacements
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LanguageToolRuleResponse(String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LanguageToolReplacementResponse(String value) {
    }
}
