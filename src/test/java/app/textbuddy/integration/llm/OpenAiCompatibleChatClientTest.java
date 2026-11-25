package app.textbuddy.integration.llm;

import app.textbuddy.advisor.AdvisorRuleCheck;
import app.textbuddy.config.TextbuddyProperties;
import app.textbuddy.quickaction.SummarizePrompt;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleChatClientTest {

    private static final AdvisorRuleCheck RULE_CHECK = new AdvisorRuleCheck(
            "schreibweisungen", "Schreibweisungen", "/api/advisor/doc/schreibweisungen#page=1",
            "per-sofort-vermeiden", "Per sofort ersetzen", 1,
            "Verwende die hausinterne Schreibweise.", "Vermeide per sofort.", "ab sofort", List.of("per sofort")
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void summarizesThroughTheConfiguredEndpointWithTheRequestedPrompt() throws Exception {
        String text = "  Der Gemeinderat bewilligt das Gesuch.\nDas Bauamt informiert die Gemeinde.  ";
        String summary = "Der Gemeinderat bewilligt das Gesuch.";
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            assertThat(exchange.getRequestURI().getPath()).isEqualTo("/v1/chat/completions");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-token");

            JsonNode request = readRequestJson(exchange);
            assertThat(request.path("model").asString()).isEqualTo("test-model");
            assertThat(request.path("stream").asBoolean()).isFalse();
            JsonNode system = request.path("messages").path(0);
            JsonNode user = request.path("messages").path(1);
            assertThat(system.path("role").asString()).isEqualTo("system");
            assertThat(system.path("content").asString()).contains("exactly one sentence long", "de-CH");
            assertThat(user.path("role").asString()).isEqualTo("user");
            assertThat(user.path("content").asString()).contains(text);
            writeJson(exchange, 200, """
                    {"choices":[{"finish_reason":"stop","message":{"content":%s}}]}
                    """.formatted(objectMapper.writeValueAsString(summary)));
        });

        assertThat(newAdapter().rewrite(text, "de-CH", SummarizePrompt.SENTENCE)).isEqualTo(summary);
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void parsesJsonInsideCodeFencesWithoutAnotherProviderCall() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        startServer(exchange -> {
            requestCount.incrementAndGet();
            writeJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"```json\\n{\\\"matches\\\":[{\\\"ruleId\\\":\\\"rule-1\\\"}]}\\n```\"}}]}");
        });

        JsonNode response = newClient().completeJson("System", "User");

        assertThat(response.path("matches").path(0).path("ruleId").asString()).isEqualTo("rule-1");
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void rejectsATruncatedProviderResponseEvenWhenItsContentIsValidJson() throws Exception {
        startServer(exchange -> writeJson(exchange, 200,
                "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"[]\"}}]}"));

        assertThatThrownBy(() -> newAdapter().validate("Die Bewilligung gilt per sofort.", List.of(RULE_CHECK)))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("unvollständige Antwort");
    }

    @Test
    void rejectsAnAdvisorFindingWithoutAnOriginalTextRange() throws Exception {
        String response = """
                {"matches":[{"documentName":"schreibweisungen","ruleId":"per-sofort-vermeiden",
                "message":"Vermeide diese Formulierung.","suggestion":"ab sofort"}]}
                """;
        startServer(exchange -> writeJson(exchange, 200, """
                {"choices":[{"finish_reason":"stop","message":{"content":%s}}]}
                """.formatted(objectMapper.writeValueAsString(response))));

        assertThatThrownBy(() -> newAdapter().validate("Die Bewilligung gilt per sofort.", List.of(RULE_CHECK)))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("unvollständige Advisor-Befunde");
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

        assertThatThrownBy(() -> newAdapter().rewrite("Der Entscheid ist relevant.", "de-CH", null))
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

    private OpenAiTextbuddyLlmClient newAdapter() {
        return new OpenAiTextbuddyLlmClient(
                newClient(), new LlmPrompts(new PromptCatalog(new DefaultResourceLoader()), objectMapper)
        );
    }

    private OpenAiCompatibleChatClient newClient() {
        TextbuddyProperties.Llm properties = new TextbuddyProperties.Llm();
        properties.setMode(TextbuddyProperties.Llm.Mode.PROVIDER);
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/models");
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
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
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
