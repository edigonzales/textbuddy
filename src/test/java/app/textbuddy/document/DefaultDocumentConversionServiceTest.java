package app.textbuddy.document;

import app.textbuddy.config.DocumentImportProperties;
import app.textbuddy.integration.docling.DoclingClient;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultDocumentConversionServiceTest {

    @Test
    void acceptsSupportedFileTypesByExtension() {
        AtomicBoolean called = new AtomicBoolean(false);
        DoclingClient doclingClient = (upload, ocrLanguage) -> {
            called.set(true);
            return "<h1>Import</h1><p>Dokument</p>";
        };
        DefaultDocumentConversionService service = createService(doclingClient);

        DocumentConversionResponse response = service.convert(new DocumentUpload(
                "brief.docx",
                "application/octet-stream",
                "dummy".getBytes(StandardCharsets.UTF_8)
        ), "en");

        assertThat(called).isTrue();
        assertThat(response.html()).contains("<h1>Import</h1>");
    }

    @Test
    void rejectsUnsupportedFileTypesWithoutCallingDocling() {
        AtomicBoolean called = new AtomicBoolean(false);
        DoclingClient doclingClient = (upload, ocrLanguage) -> {
            called.set(true);
            return "<p>ignored</p>";
        };
        DefaultDocumentConversionService service = createService(doclingClient);

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
        DefaultDocumentConversionService service = createService(doclingClient);

        assertThatThrownBy(() -> service.convert(new DocumentUpload(
                "leer.txt",
                "text/plain",
                new byte[0]
        ), "de"))
                .isInstanceOf(EmptyDocumentUploadException.class)
                .hasMessageContaining("nicht leere Datei");
    }

    @Test
    void normalizesUnsupportedOcrLanguagesToDefault() {
        AtomicReference<String> capturedLanguage = new AtomicReference<>();
        DoclingClient doclingClient = (upload, ocrLanguage) -> {
            capturedLanguage.set(ocrLanguage);
            return "<p>Import</p>";
        };
        DefaultDocumentConversionService service = createService(doclingClient);

        service.convert(new DocumentUpload(
                "scan.png",
                "image/png",
                "dummy".getBytes(StandardCharsets.UTF_8)
        ), "xx");

        assertThat(capturedLanguage.get()).isEqualTo("de");
    }

    @Test
    void rejectsUploadsAboveConfiguredLimit() {
        AtomicBoolean called = new AtomicBoolean(false);
        DoclingClient doclingClient = (upload, ocrLanguage) -> {
            called.set(true);
            return "<p>ignored</p>";
        };
        DocumentImportProperties properties = new DocumentImportProperties();
        properties.setMode(DocumentImportProperties.Mode.STUB);
        properties.setMaxUploadSize(DataSize.ofBytes(4));
        DefaultDocumentConversionService service = new DefaultDocumentConversionService(
                doclingClient,
                new DocumentImportFormatCatalog(properties),
                properties,
                new EditorFriendlyHtmlPostProcessor()
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

    private DefaultDocumentConversionService createService(DoclingClient doclingClient) {
        DocumentImportProperties properties = new DocumentImportProperties();
        properties.setMode(DocumentImportProperties.Mode.STUB);

        return new DefaultDocumentConversionService(
                doclingClient,
                new DocumentImportFormatCatalog(properties),
                properties,
                new EditorFriendlyHtmlPostProcessor()
        );
    }
}
