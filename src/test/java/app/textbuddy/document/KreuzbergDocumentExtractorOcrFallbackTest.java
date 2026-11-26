package app.textbuddy.document;

import dev.kreuzberg.ErrorCode;
import dev.kreuzberg.KreuzbergException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KreuzbergDocumentExtractorOcrFallbackTest {

    @Test
    void retriesWithDefaultLanguageWhenNonDefaultOcrLanguageFails() {
        List<String> attemptedLanguages = new ArrayList<>();
        KreuzbergDocumentExtractor client = new KreuzbergDocumentExtractor(45, (content, mimeType, extractionConfig) -> {
            attemptedLanguages.add(extractionConfig.getOcr().getLanguage());
            throw new KreuzbergException("OCR failed", ErrorCode.OCR_ERROR);
        });

        assertThatThrownBy(() -> client.extractText(
                new DocumentUpload("scan.png", "image/png", "noop".getBytes(StandardCharsets.UTF_8)),
                "fr"
        ))
                .isInstanceOf(DocumentConversionFailedException.class)
                .hasMessageContaining("OCR konnte");

        assertThat(attemptedLanguages).containsExactly("fra", "deu");
    }

    @Test
    void doesNotRetryWhenDefaultLanguageFails() {
        List<String> attemptedLanguages = new ArrayList<>();
        KreuzbergDocumentExtractor client = new KreuzbergDocumentExtractor(45, (content, mimeType, extractionConfig) -> {
            attemptedLanguages.add(extractionConfig.getOcr().getLanguage());
            throw new KreuzbergException("OCR failed", ErrorCode.OCR_ERROR);
        });

        assertThatThrownBy(() -> client.extractText(
                new DocumentUpload("scan.png", "image/png", "noop".getBytes(StandardCharsets.UTF_8)),
                "de"
        ))
                .isInstanceOf(DocumentConversionFailedException.class)
                .hasMessageContaining("OCR konnte");

        assertThat(attemptedLanguages).containsExactly("deu");
    }

    @Test
    void mapsTimeoutErrorsToControlledTimeoutException() {
        KreuzbergDocumentExtractor client = new KreuzbergDocumentExtractor(45, (content, mimeType, extractionConfig) -> {
            throw new KreuzbergException("extraction timeout reached", ErrorCode.GENERIC_ERROR);
        });

        assertThatThrownBy(() -> client.extractText(
                new DocumentUpload("scan.pdf", "application/pdf", "noop".getBytes(StandardCharsets.UTF_8)),
                "en"
        ))
                .isInstanceOf(DocumentImportTimeoutException.class)
                .hasMessageContaining("Zeitlimit");
    }

    @Test
    void mapsParsingErrorsToControlledCorruptionMessage() {
        KreuzbergDocumentExtractor client = new KreuzbergDocumentExtractor(45, (content, mimeType, extractionConfig) -> {
            throw new KreuzbergException("cannot parse", ErrorCode.PARSING_ERROR);
        });

        assertThatThrownBy(() -> client.extractText(
                new DocumentUpload("kaputt.pdf", "application/pdf", "noop".getBytes(StandardCharsets.UTF_8)),
                "de"
        ))
                .isInstanceOf(DocumentConversionFailedException.class)
                .hasMessageContaining("beschädigt");
    }
}
