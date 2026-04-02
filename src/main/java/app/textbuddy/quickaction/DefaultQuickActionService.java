package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.BulletPointsLlmClient;
import app.textbuddy.integration.llm.PlainLanguageLlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

@Service
public class DefaultQuickActionService implements QuickActionService {

    private static final String PLAIN_LANGUAGE_ERROR_MESSAGE = "Plain-Language-Rewrite konnte nicht erstellt werden.";
    private static final String BULLET_POINTS_ERROR_MESSAGE = "Bullet-Points-Rewrite konnte nicht erstellt werden.";

    private final PlainLanguageLlmClient plainLanguageLlmClient;
    private final BulletPointsLlmClient bulletPointsLlmClient;

    public DefaultQuickActionService(
            PlainLanguageLlmClient plainLanguageLlmClient,
            BulletPointsLlmClient bulletPointsLlmClient
    ) {
        this.plainLanguageLlmClient = plainLanguageLlmClient;
        this.bulletPointsLlmClient = bulletPointsLlmClient;
    }

    @Override
    public void streamPlainLanguage(QuickActionStreamRequest request, QuickActionStreamHandler handler) {
        streamRewrite(request, handler, plainLanguageLlmClient::streamPlainLanguage, PLAIN_LANGUAGE_ERROR_MESSAGE);
    }

    @Override
    public void streamBulletPoints(QuickActionStreamRequest request, QuickActionStreamHandler handler) {
        streamRewrite(request, handler, bulletPointsLlmClient::streamBulletPoints, BULLET_POINTS_ERROR_MESSAGE);
    }

    private void streamRewrite(
            QuickActionStreamRequest request,
            QuickActionStreamHandler handler,
            BiFunction<String, String, List<String>> streamer,
            String errorMessage
    ) {
        String original = normalize(request == null ? null : request.text());
        String language = normalize(request == null ? null : request.language());

        if (original.isBlank()) {
            handler.complete("");
            return;
        }

        try {
            List<String> chunks = streamer.apply(original, language);
            StringBuilder completeText = new StringBuilder();

            for (String chunk : chunks) {
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }

                handler.chunk(chunk);
                completeText.append(chunk);
            }

            if (completeText.isEmpty()) {
                completeText.append(original);
                handler.chunk(original);
            }

            handler.complete(completeText.toString());
        } catch (RuntimeException exception) {
            handler.error(errorMessage);
        }
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
