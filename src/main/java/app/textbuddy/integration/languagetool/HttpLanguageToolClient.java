package app.textbuddy.integration.languagetool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

public final class HttpLanguageToolClient implements LanguageToolClient {

    private final RestClient restClient;

    public HttpLanguageToolClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<LanguageToolMatch> check(String text, String language) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("text", text);
        form.add("language", language);

        LanguageToolCheckResponse response = restClient.post()
                .uri("/v2/check")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(LanguageToolCheckResponse.class);

        if (response == null || response.matches() == null) {
            return List.of();
        }

        return response.matches().stream()
                .map(this::mapMatch)
                .toList();
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
