package app.textbuddy.document;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public enum DocumentOcrLanguage {
    DE("de", "Deutsch"),
    EN("en", "English"),
    FR("fr", "Français"),
    IT("it", "Italiano");

    public static final DocumentOcrLanguage DEFAULT = DE;

    private final String code;
    private final String label;

    DocumentOcrLanguage(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static String normalizeCode(String requestedLanguage) {
        String normalized = Objects.requireNonNullElse(requestedLanguage, "")
                .trim()
                .toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter((language) -> language.code.equals(normalized))
                .findFirst()
                .orElse(DEFAULT)
                .code;
    }

    public static List<DocumentOcrLanguage> supportedLanguages() {
        return List.of(values());
    }
}
