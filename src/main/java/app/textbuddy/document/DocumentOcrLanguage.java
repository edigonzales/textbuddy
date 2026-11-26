package app.textbuddy.document;

import java.util.Locale;
import java.util.Objects;

public final class DocumentOcrLanguage {
    public static final String DEFAULT = "deu";

    private DocumentOcrLanguage() {
    }

    public static String tesseractCode(String requestedLanguage) {
        return switch (Objects.requireNonNullElse(requestedLanguage, "").trim().toLowerCase(Locale.ROOT)) {
            case "en" -> "eng";
            case "fr" -> "fra";
            case "it" -> "ita";
            default -> DEFAULT;
        };
    }
}
