package app.textbuddy.integration.llm;

import app.textbuddy.config.TextbuddyProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class OpenAiCompatibleChatClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleChatClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TextbuddyProperties.Llm properties;

    public OpenAiCompatibleChatClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            TextbuddyProperties.Llm properties
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
        HttpRequest request = buildRequest(systemPrompt, userPrompt);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            ensureSuccessfulStatus(response.statusCode());

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractMessageContent(root);

            if (content.isBlank()) {
                throw new LlmProviderException("LLM-Provider lieferte keinen Antworttext.");
            }

            return content;
        } catch (IOException exception) {
            throw new LlmProviderException("LLM-Antwort konnte nicht gelesen werden.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException("LLM-Anfrage wurde unterbrochen.", exception);
        }
    }

    public JsonNode completeJson(String systemPrompt, String userPrompt) {
        String response = completeText(systemPrompt, userPrompt);

        try {
            return parseEmbeddedJson(response);
        } catch (IOException exception) {
            throw new LlmProviderException("LLM-Antwort enthielt kein gültiges JSON.", exception);
        }
    }

    private HttpRequest buildRequest(String systemPrompt, String userPrompt) {
        String jsonBody = toJson(new ChatCompletionRequest(
                properties.getModel(),
                List.of(
                        new ChatMessage("system", normalize(systemPrompt)),
                        new ChatMessage("user", normalize(userPrompt))
                ),
                properties.normalizedTemperature(),
                false
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

    private void ensureSuccessfulStatus(int statusCode) {
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

        throw new LlmProviderException(message);
    }

    private String extractMessageContent(JsonNode root) throws IOException {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
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

        throw new IOException("Unbekanntes LLM-Content-Format.");
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

}
