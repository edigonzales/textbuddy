package app.textbuddy.quickaction;

public record QuickActionRequest(
        String text,
        String language,
        String option,
        String prompt
) {
}
