package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentConversionFailedException;
import app.textbuddy.document.DocumentImportTimeoutException;
import app.textbuddy.document.DocumentUpload;
import dev.kreuzberg.ErrorCode;
import dev.kreuzberg.KreuzbergException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KreuzbergDoclingClientOcrFallbackTest {

    @Test
    void retriesWithDefaultLanguageWhenNonDefaultOcrLanguageFails() {
        List<String> attemptedLanguages = new ArrayList<>();
        KreuzbergDoclingClient client = new KreuzbergDoclingClient(45, (content, mimeType, extractionConfig) -> {
            attemptedLanguages.add(extractionConfig.getOcr().getLanguage());
            throw new KreuzbergException("OCR failed", ErrorCode.OCR_ERROR);
        });

        assertThatThrownBy(() -> client.convertToHtml(
                new DocumentUpload("scan.png", "image/png", "noop".getBytes(StandardCharsets.UTF_8)),
                "fr"
        ))
                .isInstanceOf(DocumentConversionFailedException.class)
                .hasMessageContaining("OCR konnte");

        assertThat(attemptedLanguages).containsExactly("fr", "de");
    }

    @Test
    void doesNotRetryWhenDefaultLanguageFails() {
        List<String> attemptedLanguages = new ArrayList<>();
        KreuzbergDoclingClient client = new KreuzbergDoclingClient(45, (content, mimeType, extractionConfig) -> {
            attemptedLanguages.add(extractionConfig.getOcr().getLanguage());
            throw new KreuzbergException("OCR failed", ErrorCode.OCR_ERROR);
        });

        assertThatThrownBy(() -> client.convertToHtml(
                new DocumentUpload("scan.png", "image/png", "noop".getBytes(StandardCharsets.UTF_8)),
                "de"
        ))
                .isInstanceOf(DocumentConversionFailedException.class)
                .hasMessageContaining("OCR konnte");

        assertThat(attemptedLanguages).containsExactly("de");
    }

    @Test
    void mapsTimeoutErrorsToControlledTimeoutException() {
        KreuzbergDoclingClient client = new KreuzbergDoclingClient(45, (content, mimeType, extractionConfig) -> {
            throw new KreuzbergException("extraction timeout reached", ErrorCode.GENERIC_ERROR);
        });

        assertThatThrownBy(() -> client.convertToHtml(
                new DocumentUpload("scan.pdf", "application/pdf", "noop".getBytes(StandardCharsets.UTF_8)),
                "en"
        ))
                .isInstanceOf(DocumentImportTimeoutException.class)
                .hasMessageContaining("Zeitlimit");
    }

    @Test
    void mapsParsingErrorsToControlledCorruptionMessage() {
        KreuzbergDoclingClient client = new KreuzbergDoclingClient(45, (content, mimeType, extractionConfig) -> {
            throw new KreuzbergException("cannot parse", ErrorCode.PARSING_ERROR);
        });

        assertThatThrownBy(() -> client.convertToHtml(
                new DocumentUpload("kaputt.pdf", "application/pdf", "noop".getBytes(StandardCharsets.UTF_8)),
                "de"
        ))
                .isInstanceOf(DocumentConversionFailedException.class)
                .hasMessageContaining("beschädigt");
    }
}
