package app.textbuddy.integration.llm;

import app.textbuddy.config.LlmProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class OpenAiCompatibleChatClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleChatClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final LlmProperties properties;

    public OpenAiCompatibleChatClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            LlmProperties properties
    ) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.properties = Objects.requireNonNull(properties);

        log.info(
                "LLM client: provider mode ({}, model={})",
                properties.normalizedBaseUrl(),
                properties.getModel()
        );
    }

    public String completeText(String systemPrompt, String userPrompt) {
        return withTransportRetry(() -> completeTextOnce(systemPrompt, userPrompt));
    }

    public JsonNode completeJson(String systemPrompt, String userPrompt) {
        String currentPrompt = userPrompt;

        for (int attempt = 0; attempt < 2; attempt += 1) {
            String response = completeText(systemPrompt, currentPrompt);

            try {
                return parseEmbeddedJson(response);
            } catch (IOException exception) {
                if (attempt >= 1) {
                    throw new LlmProviderException("LLM-Antwort enthielt kein gültiges JSON.", exception);
                }

                currentPrompt = userPrompt + "\n\n"
                        + "Wichtig: Antworte jetzt ausschließlich mit gültigem JSON. "
                        + "Kein Markdown, keine Code-Fences, keine zusätzlichen Erläuterungen.";
            }
        }

        throw new LlmProviderException("LLM-Antwort enthielt kein gültiges JSON.");
    }

    public List<String> streamText(String systemPrompt, String userPrompt) {
        return withTransportRetry(() -> streamTextOnce(systemPrompt, userPrompt));
    }

    private String completeTextOnce(String systemPrompt, String userPrompt) {
        HttpRequest request = buildRequest(systemPrompt, userPrompt, false);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            ensureSuccessfulStatus(response.statusCode(), response.body());

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractMessageContent(root);

            if (content.isBlank()) {
                throw new LlmProviderException("LLM-Provider lieferte keinen Antworttext.");
            }

            return content;
        } catch (RetriableTransportException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RetriableTransportException("LLM-Antwort konnte nicht gelesen werden.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException("LLM-Anfrage wurde unterbrochen.", exception);
        }
    }

    private List<String> streamTextOnce(String systemPrompt, String userPrompt) {
        HttpRequest request = buildRequest(systemPrompt, userPrompt, true);

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                ensureSuccessfulStatus(response.statusCode(), readBody(response.body()));
            }

            List<String> chunks = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8)
            )) {
                String line;

                while ((line = reader.readLine()) != null) {
                    String normalized = line.trim();

                    if (normalized.isEmpty() || !normalized.startsWith("data:")) {
                        continue;
                    }

                    String data = normalized.substring("data:".length()).trim();

                    if ("[DONE]".equals(data)) {
                        break;
                    }

                    JsonNode event = objectMapper.readTree(data);
                    String content = extractStreamDeltaContent(event);

                    if (!content.isEmpty()) {
                        chunks.add(content);
                    }
                }
            }

            return List.copyOf(chunks);
        } catch (RetriableTransportException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RetriableTransportException("LLM-Stream konnte nicht gelesen werden.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException("LLM-Stream wurde unterbrochen.", exception);
        }
    }

    private HttpRequest buildRequest(String systemPrompt, String userPrompt, boolean stream) {
        String jsonBody = toJson(new ChatCompletionRequest(
                properties.getModel(),
                List.of(
                        new ChatMessage("system", normalize(systemPrompt)),
                        new ChatMessage("user", normalize(userPrompt))
                ),
                properties.normalizedTemperature(),
                stream
        ));

        Duration timeout = properties.normalizedTimeout();
        String endpoint = properties.normalizedBaseUrl() + "/chat/completions";

        return HttpRequest.newBuilder()
                .uri(java.net.URI.create(endpoint))
                .timeout(timeout)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
    }

    private <T> T withTransportRetry(Supplier<T> supplier) {
        int attempts = properties.normalizedMaxRetries() + 1;
        RetriableTransportException lastException = null;

        for (int attempt = 1; attempt <= attempts; attempt += 1) {
            try {
                return supplier.get();
            } catch (RetriableTransportException exception) {
                lastException = exception;

                if (attempt >= attempts) {
                    break;
                }

                log.warn("LLM-Aufruf fehlgeschlagen, neuer Versuch {}/{}.", attempt, attempts, exception);
            }
        }

        if (lastException != null) {
            throw new LlmProviderException(lastException.getMessage(), lastException.getCause());
        }

        throw new LlmProviderException("LLM-Aufruf ist fehlgeschlagen.");
    }

    private void ensureSuccessfulStatus(int statusCode, String body) {
        if (statusCode < 400) {
            return;
        }

        String message = switch (statusCode) {
            case 401, 403 -> "LLM-Provider lehnt die Anmeldedaten ab.";
            case 429 -> "LLM-Provider hat das Rate Limit erreicht.";
            default -> statusCode >= 500
                    ? "LLM-Provider ist momentan nicht verfügbar."
                    : "LLM-Provider antwortete mit HTTP " + statusCode + ".";
        };

        if (statusCode == 429 || statusCode >= 500) {
            throw new RetriableTransportException(message + compactBodySuffix(body), null);
        }

        throw new LlmProviderException(message + compactBodySuffix(body));
    }

    private String compactBodySuffix(String body) {
        String normalized = normalize(body);

        if (normalized.isBlank()) {
            return "";
        }

        String compact = normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "…";
        return " Antwort: " + compact;
    }

    private String extractMessageContent(JsonNode root) throws IOException {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        return extractContentValue(contentNode);
    }

    private String extractStreamDeltaContent(JsonNode root) throws IOException {
        JsonNode contentNode = root.path("choices").path(0).path("delta").path("content");
        return extractContentValue(contentNode);
    }

    private String extractContentValue(JsonNode contentNode) throws IOException {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }

        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }

        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();

            for (JsonNode element : contentNode) {
                if (element.isTextual()) {
                    builder.append(element.asText(""));
                    continue;
                }

                JsonNode textNode = element.path("text");
                if (textNode.isTextual()) {
                    builder.append(textNode.asText(""));
                }
            }

            return builder.toString();
        }

        throw new IOException("Unbekanntes LLM-Content-Format: " + contentNode);
    }

    private JsonNode parseEmbeddedJson(String response) throws IOException {
        String trimmed = normalize(response);

        if (trimmed.startsWith("```")) {
            trimmed = stripCodeFence(trimmed);
        }

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return objectMapper.readTree(trimmed);
        }

        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start = resolveJsonStart(objectStart, arrayStart);

        if (start >= 0) {
            char open = trimmed.charAt(start);
            char close = open == '{' ? '}' : ']';
            int end = trimmed.lastIndexOf(close);

            if (end > start) {
                return objectMapper.readTree(trimmed.substring(start, end + 1));
            }
        }

        return objectMapper.readTree(trimmed);
    }

    private int resolveJsonStart(int objectStart, int arrayStart) {
        if (objectStart < 0) {
            return arrayStart;
        }

        if (arrayStart < 0) {
            return objectStart;
        }

        return Math.min(objectStart, arrayStart);
    }

    private String stripCodeFence(String value) {
        String normalized = value;

        if (normalized.startsWith("```json")) {
            normalized = normalized.substring("```json".length()).trim();
        } else if (normalized.startsWith("```")) {
            normalized = normalized.substring(3).trim();
        }

        if (normalized.endsWith("```")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
        }

        return normalized;
    }

    private String toJson(ChatCompletionRequest payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("LLM-Request konnte nicht serialisiert werden.", exception);
        }
    }

    private String readBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            double temperature,
            boolean stream
    ) {
    }

    private record ChatMessage(String role, String content) {
    }

    private static final class RetriableTransportException extends RuntimeException {

        private RetriableTransportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
