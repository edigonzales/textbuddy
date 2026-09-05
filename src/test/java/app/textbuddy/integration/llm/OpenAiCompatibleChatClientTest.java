package app.textbuddy.integration.llm;

import app.textbuddy.config.TextbuddyProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
    void completesTextWithTheConfiguredEndpointAndCredentials() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            assertThat(exchange.getRequestURI().getPath()).isEqualTo("/v1/chat/completions");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-token");

            JsonNode request = readRequestJson(exchange);
            assertThat(request.path("model").asString()).isEqualTo("test-model");
            assertThat(request.path("stream").asBoolean()).isFalse();
            assertThat(request.path("messages").path(0).path("role").asString()).isEqualTo("system");
            assertThat(request.path("messages").path(1).path("role").asString()).isEqualTo("user");
            writeJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"Antworttext\"}}]}");
        });

        assertThat(newClient().completeText("System", "User")).isEqualTo("Antworttext");
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void parsesJsonInsideCodeFencesWithoutAnotherProviderCall() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"```json\\n{\\\"synonyms\\\":[\\\"rasch\\\"]}\\n```\"}}]}");
        });

        JsonNode response = newClient().completeJson("System", "User");

        assertThat(response.path("synonyms").path(0).asString()).isEqualTo("rasch");
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void doesNotRetryInvalidJson() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"kein JSON\"}}]}");
        });

        assertThatThrownBy(() -> newClient().completeJson("System", "User"))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("gültiges JSON");
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void doesNotRetryRateLimitsOrExposeTheProviderBody() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeJson(exchange, 429, "{\"error\":\"secret-provider-detail\"}");
        });

        assertThatThrownBy(() -> newClient().completeText("System", "User"))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("Rate Limit")
                .hasMessageNotContaining("secret-provider-detail");
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void mapsUnauthorizedResponsesWithoutExposingTheirBody() throws Exception {
        startServer(exchange -> writeJson(exchange, 401, "{\"error\":\"secret\"}"));

        assertThatThrownBy(() -> newClient().completeText("System", "User"))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("Anmeldedaten")
                .hasMessageNotContaining("secret");
    }

    private OpenAiCompatibleChatClient newClient() {
        TextbuddyProperties.Llm properties = new TextbuddyProperties.Llm();
        properties.setMode(TextbuddyProperties.Llm.Mode.PROVIDER);
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v1/models");
        properties.setApiKey("test-token");
        properties.setModel("test-model");
        properties.setTimeout(Duration.ofSeconds(5));
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

    private JsonNode readRequestJson(HttpExchange exchange) throws IOException {
        return objectMapper.readTree(exchange.getRequestBody().readAllBytes());
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
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
