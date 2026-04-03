package app.textbuddy.quickaction;

public record FormalityQuickActionRequest(
        String text,
        String language,
        String option
) {
}
