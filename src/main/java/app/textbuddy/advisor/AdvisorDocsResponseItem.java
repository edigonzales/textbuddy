package app.textbuddy.advisor;

public record AdvisorDocsResponseItem(
        String name,
        String title,
        String summary,
        String source,
        String documentUrl
) {
}
