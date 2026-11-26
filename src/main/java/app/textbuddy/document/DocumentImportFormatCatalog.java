package app.textbuddy.document;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DocumentImportFormatCatalog {

    private static final List<DocumentImportFormat> FORMATS = List.of(
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
            new DocumentImportFormat(".tif", "TIF", List.of("image/tiff")),
            new DocumentImportFormat(".tiff", "TIFF", List.of("image/tiff"))
    );

    public List<String> labels() {
        return FORMATS.stream()
                .map(DocumentImportFormat::label)
                .toList();
    }

    public String acceptAttribute() {
        return FORMATS.stream()
                .map(DocumentImportFormat::extension)
                .collect(Collectors.joining(","));
    }

    public String describeSupportedFormats() {
        return String.join(", ", labels());
    }

    public boolean supports(String filename, String contentType) {
        return FORMATS.stream().anyMatch((format) ->
                format.matchesExtension(Objects.requireNonNullElse(filename, ""))
                        || format.matchesContentType(contentType)
        );
    }
}
