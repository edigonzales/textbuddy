package app.textbuddy.document;

import app.textbuddy.config.DocumentImportProperties;
import dev.kreuzberg.Kreuzberg;
import dev.kreuzberg.KreuzbergException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DocumentImportFormatCatalog {

    private static final Logger log = LoggerFactory.getLogger(DocumentImportFormatCatalog.class);

    private final List<DocumentImportFormat> formats;

    public DocumentImportFormatCatalog(DocumentImportProperties properties) {
        this.formats = resolveFormats(properties);
    }

    public List<DocumentImportFormat> listFormats() {
        return formats;
    }

    public List<String> labels() {
        return formats.stream()
                .map(DocumentImportFormat::label)
                .toList();
    }

    public String acceptAttribute() {
        return formats.stream()
                .map(DocumentImportFormat::extension)
                .collect(Collectors.joining(","));
    }

    public String describeSupportedFormats() {
        return String.join(", ", labels());
    }

    public boolean supports(String filename, String contentType) {
        return formats.stream().anyMatch((format) ->
                format.matchesExtension(Objects.requireNonNullElse(filename, ""))
                        || format.matchesContentType(contentType)
        );
    }

    private List<DocumentImportFormat> resolveFormats(DocumentImportProperties properties) {
        List<DocumentImportFormat> defaultFormats = defaultFormats();

        if (properties == null || !properties.isKreuzbergMode()) {
            return defaultFormats;
        }

        List<DocumentImportFormat> runtimeFormats = new ArrayList<>();

        for (DocumentImportFormat format : defaultFormats) {
            if (isSupportedByKreuzberg(format)) {
                runtimeFormats.add(format);
            }
        }

        if (runtimeFormats.isEmpty()) {
            log.warn("Document import: runtime format probe returned no formats, keeping defaults.");
            return defaultFormats;
        }

        return List.copyOf(runtimeFormats);
    }

    private boolean isSupportedByKreuzberg(DocumentImportFormat format) {
        if (format.contentTypes().isEmpty()) {
            return true;
        }

        for (String contentType : format.contentTypes()) {
            if (isSupportedMimeType(contentType)) {
                return true;
            }
        }

        log.info("Document import: disabling format {} because runtime does not support declared MIME types.", format.label());
        return false;
    }

    private boolean isSupportedMimeType(String mimeType) {
        try {
            String validated = Kreuzberg.validateMimeType(mimeType);
            return validated != null && !validated.isBlank();
        } catch (KreuzbergException exception) {
            return false;
        }
    }

    private List<DocumentImportFormat> defaultFormats() {
        return List.of(
                new DocumentImportFormat(".pdf", "PDF", List.of("application/pdf")),
                new DocumentImportFormat(".docx", "DOCX", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
                new DocumentImportFormat(".pptx", "PPTX", List.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
                new DocumentImportFormat(".xlsx", "XLSX", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
                new DocumentImportFormat(".html", "HTML", List.of("text/html", "application/xhtml+xml")),
                new DocumentImportFormat(".md", "Markdown", List.of("text/markdown", "text/x-markdown")),
                new DocumentImportFormat(".adoc", "AsciiDoc", List.of("text/asciidoc", "text/x-asciidoc", "application/asciidoc")),
                new DocumentImportFormat(".txt", "TXT", List.of("text/plain")),
                new DocumentImportFormat(".png", "PNG", List.of("image/png")),
                new DocumentImportFormat(".jpg", "JPG", List.of("image/jpeg")),
                new DocumentImportFormat(".jpeg", "JPEG", List.of("image/jpeg")),
                new DocumentImportFormat(".tif", "TIFF", List.of("image/tiff")),
                new DocumentImportFormat(".tiff", "TIFF", List.of("image/tiff"))
        );
    }
}
