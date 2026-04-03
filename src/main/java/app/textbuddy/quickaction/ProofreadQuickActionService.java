package app.textbuddy.quickaction;

import app.textbuddy.integration.llm.ProofreadLlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ProofreadQuickActionService {

    private static final String ERROR_MESSAGE = "Proofread konnte nicht erstellt werden.";

    private final ProofreadLlmClient proofreadLlmClient;

    public ProofreadQuickActionService(ProofreadLlmClient proofreadLlmClient) {
        this.proofreadLlmClient = proofreadLlmClient;
    }

    public void stream(QuickActionStreamRequest request, QuickActionStreamHandler handler) {
        String original = normalize(request == null ? null : request.text());
        String language = normalize(request == null ? null : request.language());

        if (original.isBlank()) {
            handler.complete("");
            return;
        }

        try {
            List<String> chunks = proofreadLlmClient.streamProofread(original, language);
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
            handler.error(ERROR_MESSAGE);
        }
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
