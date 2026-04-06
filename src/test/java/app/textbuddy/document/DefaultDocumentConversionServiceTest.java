package app.textbuddy.document;

import app.textbuddy.integration.docling.DoclingClient;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultDocumentConversionServiceTest {

    @Test
    void acceptsSupportedFileTypesByExtension() {
        AtomicBoolean called = new AtomicBoolean(false);
        DoclingClient doclingClient = upload -> {
            called.set(true);
            return "<h1>Import</h1><p>Dokument</p>";
        };
        DefaultDocumentConversionService service = new DefaultDocumentConversionService(
                doclingClient,
                new DocumentImportFormatCatalog()
        );

        DocumentConversionResponse response = service.convert(new DocumentUpload(
                "brief.docx",
                "application/octet-stream",
                "dummy".getBytes(StandardCharsets.UTF_8)
        ));

        assertThat(called).isTrue();
        assertThat(response.html()).contains("<h1>Import</h1>");
    }

    @Test
    void rejectsUnsupportedFileTypesWithoutCallingDocling() {
        AtomicBoolean called = new AtomicBoolean(false);
        DoclingClient doclingClient = upload -> {
            called.set(true);
            return "<p>ignored</p>";
        };
        DefaultDocumentConversionService service = new DefaultDocumentConversionService(
                doclingClient,
                new DocumentImportFormatCatalog()
        );

        assertThatThrownBy(() -> service.convert(new DocumentUpload(
                "payload.exe",
                "application/octet-stream",
                new byte[]{1, 2, 3}
        )))
                .isInstanceOf(UnsupportedDocumentFormatException.class)
                .hasMessageContaining("Nicht unterstütztes Dateiformat");

        assertThat(called).isFalse();
    }

    @Test
    void rejectsEmptyUploads() {
        DoclingClient doclingClient = upload -> "<p>ignored</p>";
        DefaultDocumentConversionService service = new DefaultDocumentConversionService(
                doclingClient,
                new DocumentImportFormatCatalog()
        );

        assertThatThrownBy(() -> service.convert(new DocumentUpload(
                "leer.txt",
                "text/plain",
                new byte[0]
        )))
                .isInstanceOf(EmptyDocumentUploadException.class)
                .hasMessageContaining("nicht leere Datei");
    }
}
