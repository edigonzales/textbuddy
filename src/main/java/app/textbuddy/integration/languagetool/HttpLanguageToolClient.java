package app.textbuddy.integration.languagetool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import app.textbuddy.integration.support.AdapterRetrySupport;
import app.textbuddy.integration.support.RetriableAdapterException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Locale;

public final class HttpLanguageToolClient implements LanguageToolClient {

    private final RestClient restClient;
    private final int maxRetries;

    public HttpLanguageToolClient(RestClient restClient) {
        this(restClient, 0);
    }

    public HttpLanguageToolClient(RestClient restClient, int maxRetries) {
        this.restClient = restClient;
        this.maxRetries = Math.max(0, maxRetries);
    }

    @Override
    public List<LanguageToolMatch> check(String text, String language) {
        return AdapterRetrySupport.withRetry(
                "LanguageTool",
                maxRetries,
                () -> checkOnce(text, language),
                exception -> new LanguageToolUnavailableException(exception.getMessage(), exception.getCause())
        );
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
            throw new RetriableAdapterException(
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

        if (AdapterRetrySupport.isRetriableStatusCode(statusCode)) {
            return new RetriableAdapterException(message + compactBodySuffix(exception.getResponseBodyAsString()), exception);
        }

        return new LanguageToolUnavailableException(
                message + compactBodySuffix(exception.getResponseBodyAsString()),
                exception
        );
    }

    private String compactBodySuffix(String body) {
        String normalized = normalize(body);

        if (normalized.isBlank()) {
            return "";
        }

        String compact = normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "…";
        return " Antwort: " + compact;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
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
