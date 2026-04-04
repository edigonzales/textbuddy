package app.textbuddy.quickaction;

public record MediumQuickActionRequest(
        String text,
        String language,
        String option
) {
}
