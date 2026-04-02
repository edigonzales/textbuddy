package app.textbuddy.sentencerewrite;

import java.util.List;

public record SentenceRewriteResponse(String original, List<String> alternatives) {
}
