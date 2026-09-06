package app.textbuddy.quickaction;

public record QuickActionRequest(
        String text,
        String language,
        String option,
        String prompt,
        String previousText,
        Double previousFleschScore
) {

    public QuickActionRequest(String text, String language, String option, String prompt) {
        this(text, language, option, prompt, null, null);
    }
}
