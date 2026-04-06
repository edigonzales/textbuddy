package app.textbuddy.sentencerewrite;

public record SentenceRewriteRequest(
        String sentence,
        String context
) {
}
