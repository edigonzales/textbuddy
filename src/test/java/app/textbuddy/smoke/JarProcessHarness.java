package app.textbuddy.smoke;

import org.assertj.core.api.Assertions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class JarProcessHarness implements AutoCloseable {

    private static final Duration START_TIMEOUT = Duration.ofSeconds(90);

    private final Process process;
    private final int port;
    private final StringBuilder output = new StringBuilder();
    private final Thread outputReader;
    private final HttpClient httpClient;

    private JarProcessHarness(Process process, int port) {
        this.process = process;
        this.port = port;
        this.outputReader = startOutputReader(process, output);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    static String requireBootJarPath() {
        String bootJarPath = System.getProperty("textbuddy.bootJarPath", "").trim();

        Assertions.assertThat(bootJarPath)
                .as("bootJar path")
                .isNotBlank();
        Assertions.assertThat(Path.of(bootJarPath))
                .as("bootJar file")
                .exists()
                .isRegularFile();

        return bootJarPath;
    }

    static int findFreePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setReuseAddress(true);
            return serverSocket.getLocalPort();
        }
    }

    static JarProcessHarness start(String bootJarPath, int port) throws Exception {
        Path javaBinary = Path.of(System.getProperty("java.home"), "bin", "java");
        List<String> command = new ArrayList<>();
        command.add(javaBinary.toString());
        command.add("-jar");
        command.add(bootJarPath);
        command.add("--server.port=" + port);
        command.add("--textbuddy.auth.enabled=false");
        command.add("--textbuddy.llm.mode=stub");
        command.add("--textbuddy.languagetool.mode=embedded");
        command.add("--textbuddy.document.mode=kreuzberg");

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        JarProcessHarness harness = new JarProcessHarness(process, port);
        harness.awaitStartup();
        return harness;
    }

    HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri(path))
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    HttpResponse<String> postJson(String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(baseUri(path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    HttpResponse<String> postMultipart(
            String path,
            String fieldName,
            String filename,
            String contentType,
            byte[] content
    ) throws IOException, InterruptedException {
        String boundary = "----textbuddy-boundary-" + System.nanoTime();
        String prefix = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";

        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[prefixBytes.length + content.length + suffixBytes.length];

        System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        System.arraycopy(content, 0, body, prefixBytes.length, content.length);
        System.arraycopy(suffixBytes, 0, body, prefixBytes.length + content.length, suffixBytes.length);

        HttpRequest request = HttpRequest.newBuilder(baseUri(path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    boolean isAlive() {
        return process.isAlive();
    }

    String outputSnapshot() {
        synchronized (output) {
            return output.toString();
        }
    }

    @Override
    public void close() throws Exception {
        stopProcess();
        outputReader.join(10_000L);
    }

    private URI baseUri(String path) {
        if (path.startsWith("/")) {
            return URI.create("http://127.0.0.1:" + port + path);
        }

        return URI.create("http://127.0.0.1:" + port + "/" + path);
    }

    private Thread startOutputReader(Process process, StringBuilder output) {
        return Thread.ofVirtual().start(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    synchronized (output) {
                        output.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException ignored) {
                synchronized (output) {
                    output.append("Ausgabe konnte nicht vollständig gelesen werden.").append(System.lineSeparator());
                }
            }
        });
    }

    private void awaitStartup() throws Exception {
        long deadline = System.nanoTime() + START_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException("Jar-Prozess wurde vorzeitig beendet. Ausgabe:\n" + outputSnapshot());
            }

            try {
                HttpResponse<String> healthResponse = get("/actuator/health");

                if (healthResponse.statusCode() == 200 && healthResponse.body().contains("\"status\":\"UP\"")) {
                    return;
                }
            } catch (IOException | InterruptedException ignored) {
                // Polling bis der Server lauscht.
            }

            Thread.sleep(250L);
        }

        throw new IllegalStateException("Jar ist nicht innerhalb des Zeitlimits gestartet. Ausgabe:\n" + outputSnapshot());
    }

    private void stopProcess() throws InterruptedException {
        if (!process.isAlive()) {
            return;
        }

        process.destroy();

        if (process.waitFor(10, TimeUnit.SECONDS)) {
            return;
        }

        process.destroyForcibly();
        process.waitFor(10, TimeUnit.SECONDS);
    }
}
