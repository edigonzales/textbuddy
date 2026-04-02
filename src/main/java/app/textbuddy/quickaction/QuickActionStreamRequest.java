package app.textbuddy.quickaction;

public record QuickActionStreamRequest(
        String text,
        String language,
        String option,
        String prompt
) {
}
