package app.textbuddy.document;

import app.textbuddy.config.TextbuddyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentConversionServiceTest {

    private final KreuzbergDocumentExtractor extractor = mock(KreuzbergDocumentExtractor.class);
    private final TextbuddyProperties properties = new TextbuddyProperties();
    private final DocumentConversionService service = new DocumentConversionService(
            extractor, new DocumentImportFormatCatalog(), properties);

    @Test
    void acceptsSupportedFileTypesByExtension() {
        DocumentUpload upload = new DocumentUpload("brief.docx", "application/octet-stream",
                "dummy".getBytes(StandardCharsets.UTF_8));
        when(extractor.extractText(upload, "en")).thenReturn("Import\nDokument");

        DocumentConversionResponse response = service.convert(upload, "en");

        assertThat(response.text()).isEqualTo("Import\nDokument");
    }

    @Test
    void rejectsUnsupportedFileTypesWithoutCallingExtractor() {
        assertThatThrownBy(() -> service.convert(new DocumentUpload(
                "payload.exe",
                "application/octet-stream",
                new byte[]{1, 2, 3}
        ), "de"))
                .isInstanceOf(UnsupportedDocumentFormatException.class)
                .hasMessageContaining("Nicht unterstütztes Dateiformat");

        verifyNoInteractions(extractor);
    }

    @Test
    void rejectsEmptyUploads() {
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
        properties.getDocument().setMaxUploadSize(DataSize.ofBytes(4));

        assertThatThrownBy(() -> service.convert(new DocumentUpload(
                "gross.txt",
                "text/plain",
                "12345".getBytes(StandardCharsets.UTF_8)
        ), "de"))
                .isInstanceOf(DocumentUploadTooLargeException.class)
                .hasMessageContaining("Maximal erlaubt sind");

        verifyNoInteractions(extractor);
    }
}
