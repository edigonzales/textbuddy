package app.textbuddy.integration.llm;

import app.textbuddy.config.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleChatClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void completeTextUsesAuthorizationModelAndChatCompletionsEndpoint() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            assertThat(exchange.getRequestURI().getPath()).isEqualTo("/v1/chat/completions");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-token");

            JsonNode request = readRequestJson(exchange);
            assertThat(request.path("model").asText()).isEqualTo("qwen3");
            assertThat(request.path("stream").asBoolean()).isFalse();
            assertThat(request.path("messages").path(0).path("role").asText()).isEqualTo("system");
            assertThat(request.path("messages").path(1).path("role").asText()).isEqualTo("user");

            writeJson(exchange, 200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "Antworttext"
                          }
                        }
                      ]
                    }
                    """);
        });

        OpenAiCompatibleChatClient client = newClient(serverBaseUrl());

        assertThat(client.completeText("System", "User")).isEqualTo("Antworttext");
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void streamTextParsesOpenAiCompatibleSseChunks() throws Exception {
        startServer(exchange -> {
            JsonNode request = readRequestJson(exchange);
            assertThat(request.path("stream").asBoolean()).isTrue();

            writeText(exchange, 200, "text/event-stream", """
                    data: {"choices":[{"delta":{"role":"assistant","content":""}}]}

                    data: {"choices":[{"delta":{"content":"STREAM"}}]}

                    data: {"choices":[{"delta":{"content":"_OK"}}]}

                    data: {"choices":[{"delta":{"content":""},"finish_reason":"stop"}]}

                    data: [DONE]
                    """);
        });

        OpenAiCompatibleChatClient client = newClient(serverBaseUrl());

        assertThat(client.streamText("System", "User")).containsExactly("STREAM", "_OK");
    }

    @Test
    void completeJsonRetriesOnceWhenModelReturnsInvalidJson() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            int count = requestCount.incrementAndGet();

            if (count == 1) {
                writeJson(exchange, 200, """
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "Das ist kein JSON."
                              }
                            }
                          ]
                        }
                        """);
                return;
            }

            writeJson(exchange, 200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "```json\\n{\\"synonyms\\":[\\"rasch\\"]}\\n```"
                          }
                        }
                      ]
                    }
                    """);
        });

        OpenAiCompatibleChatClient client = newClient(serverBaseUrl());

        JsonNode response = client.completeJson("System", "User");

        assertThat(response.path("synonyms").path(0).asText()).isEqualTo("rasch");
        assertThat(requestCount).hasValue(2);
    }

    @Test
    void retriesOnRateLimitResponses() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            int count = requestCount.incrementAndGet();

            if (count == 1) {
                writeJson(exchange, 429, "{\"error\":\"rate_limit\"}");
                return;
            }

            writeJson(exchange, 200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "Nach Retry"
                          }
                        }
                      ]
                    }
                    """);
        });

        OpenAiCompatibleChatClient client = newClient(serverBaseUrl());

        assertThat(client.completeText("System", "User")).isEqualTo("Nach Retry");
        assertThat(requestCount).hasValue(2);
    }

    @Test
    void throwsProviderExceptionOnUnauthorizedResponses() throws Exception {
        startServer(exchange -> writeJson(exchange, 401, "{\"error\":\"unauthorized\"}"));

        OpenAiCompatibleChatClient client = newClient(serverBaseUrl());

        assertThatThrownBy(() -> client.completeText("System", "User"))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("Anmeldedaten");
    }

    private OpenAiCompatibleChatClient newClient(String baseUrl) {
        LlmProperties properties = new LlmProperties();
        properties.setMode(LlmProperties.Mode.PROVIDER);
        properties.setBaseUrl(baseUrl);
        properties.setApiKey("test-token");
        properties.setModel("qwen3");
        properties.setTimeout(Duration.ofSeconds(5));
        properties.setMaxRetries(1);

        return new OpenAiCompatibleChatClient(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                objectMapper,
                properties
        );
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private String serverBaseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/v1/models";
    }

    private JsonNode readRequestJson(HttpExchange exchange) throws IOException {
        return objectMapper.readTree(exchange.getRequestBody().readAllBytes());
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        writeText(exchange, status, "application/json", body);
    }

    private void writeText(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
