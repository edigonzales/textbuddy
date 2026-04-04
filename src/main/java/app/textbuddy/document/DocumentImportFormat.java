package app.textbuddy.document;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record DocumentImportFormat(
        String extension,
        String label,
        List<String> contentTypes
) {

    public DocumentImportFormat {
        extension = normalizeExtension(extension);
        label = Objects.requireNonNullElse(label, "").trim();
        contentTypes = contentTypes == null
                ? List.of()
                : contentTypes.stream()
                .filter(Objects::nonNull)
                .map(DocumentImportFormat::normalizeContentType)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public boolean matchesExtension(String filename) {
        String normalizedFilename = Objects.requireNonNullElse(filename, "")
                .trim()
                .toLowerCase(Locale.ROOT);

        return !normalizedFilename.isBlank() && normalizedFilename.endsWith(extension);
    }

    public boolean matchesContentType(String contentType) {
        String normalizedContentType = normalizeContentType(contentType);
        return !normalizedContentType.isBlank() && contentTypes.contains(normalizedContentType);
    }

    private static String normalizeExtension(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return "";
        }

        return normalized.startsWith(".") ? normalized : "." + normalized;
    }

    private static String normalizeContentType(String value) {
        String normalized = Objects.requireNonNullElse(value, "")
                .trim()
                .toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(';');

        if (separator >= 0) {
            normalized = normalized.substring(0, separator).trim();
        }

        return normalized;
    }
}
