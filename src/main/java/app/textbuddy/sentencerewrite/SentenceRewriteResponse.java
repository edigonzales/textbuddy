package app.textbuddy.sentencerewrite;

import java.util.List;

public record SentenceRewriteResponse(String sentence, List<String> options) {
}
