package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentConversionFailedException;
import app.textbuddy.document.DocumentImportServiceUnavailableException;
import app.textbuddy.document.DocumentImportTimeoutException;
import app.textbuddy.document.DocumentUpload;
import app.textbuddy.integration.support.AdapterRetrySupport;
import app.textbuddy.integration.support.RetriableAdapterException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;

public final class HttpDoclingClient implements DoclingClient {

    private final RestClient restClient;
    private final String apiKey;
    private final int maxRetries;

    public HttpDoclingClient(RestClient restClient, String apiKey) {
        this(restClient, apiKey, 0);
    }

    public HttpDoclingClient(RestClient restClient, String apiKey, int maxRetries) {
        this.restClient = restClient;
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.maxRetries = Math.max(0, maxRetries);
    }

    @Override
    public String convertToHtml(DocumentUpload upload, String ocrLanguage) {
        return AdapterRetrySupport.withRetry(
                "Docling",
                maxRetries,
                () -> convertToHtmlOnce(upload),
                exception -> mapFinalFailure(exception)
        );
    }

    private String convertToHtmlOnce(DocumentUpload upload) {
        JsonNode response;

        try {
            response = restClient.post()
                    .uri("/v1/convert/file")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .headers((headers) -> {
                        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

                        if (!apiKey.isBlank()) {
                            headers.set("X-API-Key", apiKey);
                        }
                    })
                    .body(createBody(upload))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw mapHttpFailure(exception);
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new RetriableAdapterException("Docling hat nicht rechtzeitig geantwortet.", exception);
            }

            throw new RetriableAdapterException("Docling ist momentan nicht erreichbar.", exception);
        } catch (RuntimeException exception) {
            throw new DocumentImportServiceUnavailableException("Docling-Aufruf ist fehlgeschlagen.", exception);
        }

        return extractHtml(response);
    }

    private RuntimeException mapHttpFailure(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        String message = switch (statusCode) {
            case 401, 403 -> "Docling lehnt die Anmeldedaten ab.";
            case 429 -> "Docling hat das Rate Limit erreicht.";
            case 400, 404, 415, 422 -> "Docling konnte dieses Dokument nicht verarbeiten.";
            default -> statusCode >= 500
                    ? "Docling ist momentan nicht verfügbar."
                    : "Docling antwortete mit HTTP " + statusCode + ".";
        };

        if (AdapterRetrySupport.isRetriableStatusCode(statusCode)) {
            return new RetriableAdapterException(message + compactBodySuffix(exception.getResponseBodyAsString()), exception);
        }

        return new DocumentConversionFailedException(message + compactBodySuffix(exception.getResponseBodyAsString()), exception);
    }

    private RuntimeException mapFinalFailure(RetriableAdapterException exception) {
        if (isTimeout(exception)) {
            return new DocumentImportTimeoutException("Dokumentimport hat das Zeitlimit überschritten.", exception);
        }

        return new DocumentImportServiceUnavailableException(
                "Docling ist momentan nicht verfügbar.",
                exception
        );
    }

    private boolean isTimeout(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }

            String message = current.getMessage();

            if (message != null && message.toLowerCase(Locale.ROOT).contains("timeout")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private MultiValueMap<String, Object> createBody(DocumentUpload upload) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", createFilePart(upload));
        body.add("to_formats", "html");
        return body;
    }

    private HttpEntity<ByteArrayResource> createFilePart(DocumentUpload upload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveContentType(upload.contentType()));
        headers.setContentDispositionFormData("files", upload.filename());

        ByteArrayResource resource = new ByteArrayResource(upload.content()) {
            @Override
            public String getFilename() {
                return upload.filename();
            }
        };

        return new HttpEntity<>(resource, headers);
    }

    private MediaType resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String extractHtml(JsonNode response) {
        if (response == null || response.isNull()) {
            throw new DocumentConversionFailedException("Docling hat keine Antwort geliefert.");
        }

        String directDocumentHtml = response.path("document").path("html_content").asText("");

        if (!directDocumentHtml.isBlank()) {
            return directDocumentHtml;
        }

        String firstDocumentHtml = response.path("documents").path(0).path("html_content").asText("");

        if (!firstDocumentHtml.isBlank()) {
            return firstDocumentHtml;
        }

        String nestedDocumentHtml = response.path("results").path(0).path("document").path("html_content").asText("");

        if (!nestedDocumentHtml.isBlank()) {
            return nestedDocumentHtml;
        }

        throw new DocumentConversionFailedException(
                "Docling-Antwort enthält kein HTML: " + compactResponse(response)
        );
    }

    private String compactResponse(JsonNode response) {
        return response.toString();
    }

    private String compactBodySuffix(String body) {
        String normalized = body == null ? "" : body.trim();

        if (normalized.isBlank()) {
            return "";
        }

        String compact = normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "…";
        return " Antwort: " + compact;
    }
}
