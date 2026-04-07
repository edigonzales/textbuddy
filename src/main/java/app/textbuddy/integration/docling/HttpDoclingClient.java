package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentConversionFailedException;
import app.textbuddy.document.DocumentUpload;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public final class HttpDoclingClient implements DoclingClient {

    private final RestClient restClient;
    private final String apiKey;

    public HttpDoclingClient(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey == null ? "" : apiKey.strip();
    }

    @Override
    public String convertToHtml(DocumentUpload upload, String ocrLanguage) {
        try {
            JsonNode response = restClient.post()
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

            return extractHtml(response);
        } catch (RuntimeException exception) {
            throw new DocumentConversionFailedException("Docling-Konvertierung ist fehlgeschlagen.", exception);
        }
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
}
