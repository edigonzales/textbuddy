package app.textbuddy.document;

import app.textbuddy.config.TextbuddyProperties;
import app.textbuddy.integration.docling.DoclingClient;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentConversionServiceTest {

    @Test
    void acceptsSupportedFileTypesByExtension() {
        AtomicBoolean called = new AtomicBoolean(false);
        DoclingClient doclingClient = (upload, ocrLanguage) -> {
            called.set(true);
            return "Import\nDokument";
        };
        DocumentConversionService service = createService(doclingClient);

        DocumentConversionResponse response = service.convert(new DocumentUpload(
                "brief.docx",
                "application/octet-stream",
                "dummy".getBytes(StandardCharsets.UTF_8)
        ), "en");

        assertThat(called).isTrue();
        assertThat(response.text()).isEqualTo("Import\nDokument");
    }

    @Test
    void rejectsUnsupportedFileTypesWithoutCallingDocling() {
        AtomicBoolean called = new AtomicBoolean(false);
        DoclingClient doclingClient = (upload, ocrLanguage) -> {
            called.set(true);
            return "<p>ignored</p>";
        };
        DocumentConversionService service = createService(doclingClient);

        assertThatThrownBy(() -> service.convert(new DocumentUpload(
                "payload.exe",
                "application/octet-stream",
                new byte[]{1, 2, 3}
        ), "de"))
                .isInstanceOf(UnsupportedDocumentFormatException.class)
                .hasMessageContaining("Nicht unterstütztes Dateiformat");

        assertThat(called).isFalse();
    }

    @Test
    void rejectsEmptyUploads() {
        DoclingClient doclingClient = (upload, ocrLanguage) -> "<p>ignored</p>";
        DocumentConversionService service = createService(doclingClient);

        assertThatThrownBy(() -> service.convert(new DocumentUpload(
                "leer.txt",
                "text/plain",
                new byte[0]
        ), "de"))
                .isInstanceOf(EmptyDocumentUploadException.class)
                .hasMessageContaining("nicht leere Datei");
    }

    @Test
    void rejectsUploadsAboveConfiguredLimit() {
        AtomicBoolean called = new AtomicBoolean(false);
        DoclingClient doclingClient = (upload, ocrLanguage) -> {
            called.set(true);
            return "<p>ignored</p>";
        };
        TextbuddyProperties properties = new TextbuddyProperties();
        properties.getDocument().setMaxUploadSize(DataSize.ofBytes(4));
        DocumentConversionService service = new DocumentConversionService(
                doclingClient,
                new DocumentImportFormatCatalog(),
                properties
        );

        assertThatThrownBy(() -> service.convert(new DocumentUpload(
                "gross.txt",
                "text/plain",
                "12345".getBytes(StandardCharsets.UTF_8)
        ), "de"))
                .isInstanceOf(DocumentUploadTooLargeException.class)
                .hasMessageContaining("Maximal erlaubt sind");

        assertThat(called).isFalse();
    }

    private DocumentConversionService createService(DoclingClient doclingClient) {
        TextbuddyProperties properties = new TextbuddyProperties();

        return new DocumentConversionService(
                doclingClient,
                new DocumentImportFormatCatalog(),
                properties
        );
    }
}
