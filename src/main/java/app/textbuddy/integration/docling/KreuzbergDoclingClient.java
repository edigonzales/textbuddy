package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentConversionFailedException;
import app.textbuddy.document.DocumentImportTimeoutException;
import app.textbuddy.document.DocumentOcrLanguage;
import app.textbuddy.document.DocumentUpload;
import dev.kreuzberg.ErrorCode;
import dev.kreuzberg.ExtractionResult;
import dev.kreuzberg.Kreuzberg;
import dev.kreuzberg.KreuzbergException;
import dev.kreuzberg.OcrException;
import dev.kreuzberg.config.ExtractionConfig;
import dev.kreuzberg.config.OcrConfig;

import java.util.Locale;
import java.util.Objects;

public final class KreuzbergDoclingClient implements DoclingClient {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final String HTML_OUTPUT_FORMAT = "html";

    private final long extractionTimeoutSeconds;
    private final KreuzbergExtractor extractor;

    public KreuzbergDoclingClient(long extractionTimeoutSeconds) {
        this(extractionTimeoutSeconds, Kreuzberg::extractBytes);
    }

    KreuzbergDoclingClient(long extractionTimeoutSeconds, KreuzbergExtractor extractor) {
        this.extractionTimeoutSeconds = Math.max(1L, extractionTimeoutSeconds);
        this.extractor = Objects.requireNonNull(extractor);
    }

    @Override
    public String convertToHtml(DocumentUpload upload, String ocrLanguage) {
        String mimeType = resolveMimeType(upload);
        String normalizedLanguage = DocumentOcrLanguage.normalizeCode(ocrLanguage);

        try {
            return extractHtml(upload, mimeType, normalizedLanguage);
        } catch (KreuzbergException primaryException) {
            if (shouldRetryWithFallbackLanguage(primaryException, normalizedLanguage)) {
                try {
                    return extractHtml(upload, mimeType, DocumentOcrLanguage.DEFAULT.code());
                } catch (KreuzbergException fallbackException) {
                    throw mapFailure(fallbackException);
                }
            }

            throw mapFailure(primaryException);
        }
    }

    private String resolveMimeType(DocumentUpload upload) {
        String detectedMimeType = detectMimeType(upload.content());

        if (!detectedMimeType.isBlank() && !DEFAULT_MIME_TYPE.equalsIgnoreCase(detectedMimeType)) {
            return detectedMimeType;
        }

        String contentType = normalize(upload.contentType());
        return contentType.isBlank() ? DEFAULT_MIME_TYPE : contentType;
    }

    private String detectMimeType(byte[] content) {
        try {
            return normalize(Kreuzberg.detectMimeType(content));
        } catch (KreuzbergException exception) {
            return "";
        }
    }

    private String extractHtml(DocumentUpload upload, String mimeType, String ocrLanguage) throws KreuzbergException {
        ExtractionConfig extractionConfig = buildExtractionConfig(ocrLanguage);
        ExtractionResult result = extractor.extract(upload.content(), mimeType, extractionConfig);

        if (result == null) {
            throw new DocumentConversionFailedException("Lokale Dokumentkonvertierung ist fehlgeschlagen.");
        }

        return normalize(result.getContent());
    }

    private ExtractionConfig buildExtractionConfig(String ocrLanguage) {
        OcrConfig ocrConfig = OcrConfig.builder()
                .language(ocrLanguage)
                .build();

        return ExtractionConfig.builder()
                .disableOcr(false)
                .forceOcr(false)
                .outputFormat(HTML_OUTPUT_FORMAT)
                .ocr(ocrConfig)
                .extractionTimeoutSecs(extractionTimeoutSeconds)
                .build();
    }

    private RuntimeException mapFailure(KreuzbergException exception) {
        if (isTimeout(exception)) {
            return new DocumentImportTimeoutException(
                    "Dokumentimport hat das Zeitlimit überschritten. Bitte eine kleinere oder weniger komplexe Datei versuchen.",
                    exception
            );
        }

        if (isMissingDependency(exception)) {
            return new DocumentConversionFailedException(
                    "Lokale OCR ist auf dieser Runtime nicht verfügbar.",
                    exception
            );
        }

        if (isOcrProblem(exception)) {
            return new DocumentConversionFailedException(
                    "OCR konnte für dieses Dokument nicht ausgeführt werden.",
                    exception
            );
        }

        if (isLikelyCorruptedFile(exception)) {
            return new DocumentConversionFailedException(
                    "Dokument konnte nicht verarbeitet werden. Die Datei ist möglicherweise beschädigt.",
                    exception
            );
        }

        return new DocumentConversionFailedException(
                "Lokale Dokumentkonvertierung ist fehlgeschlagen.",
                exception
        );
    }

    private boolean shouldRetryWithFallbackLanguage(KreuzbergException exception, String language) {
        return !DocumentOcrLanguage.DEFAULT.code().equals(language) && isOcrProblem(exception);
    }

    private boolean isTimeout(KreuzbergException exception) {
        String message = normalize(exception.getMessage()).toLowerCase(Locale.ROOT);
        return message.contains("timeout")
                || message.contains("timed out")
                || message.contains("time limit")
                || message.contains("zeitlimit");
    }

    private boolean isLikelyCorruptedFile(KreuzbergException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return errorCode == ErrorCode.INVALID_ARGUMENT
                || errorCode == ErrorCode.PARSING_ERROR
                || errorCode == ErrorCode.IO_ERROR;
    }

    private boolean isMissingDependency(KreuzbergException exception) {
        return exception.getErrorCode() == ErrorCode.MISSING_DEPENDENCY;
    }

    private boolean isOcrProblem(KreuzbergException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        if (errorCode == ErrorCode.OCR_ERROR || exception instanceof OcrException) {
            return true;
        }

        return normalize(exception.getMessage()).toLowerCase(Locale.ROOT).contains("ocr");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @FunctionalInterface
    interface KreuzbergExtractor {
        ExtractionResult extract(byte[] content, String mimeType, ExtractionConfig extractionConfig) throws KreuzbergException;
    }
}
