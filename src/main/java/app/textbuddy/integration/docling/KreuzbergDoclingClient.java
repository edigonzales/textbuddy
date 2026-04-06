package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentConversionFailedException;
import app.textbuddy.document.DocumentUpload;
import dev.kreuzberg.ExtractionResult;
import dev.kreuzberg.Kreuzberg;
import dev.kreuzberg.KreuzbergException;
import dev.kreuzberg.config.ExtractionConfig;

public final class KreuzbergDoclingClient implements DoclingClient {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final ExtractionConfig EXTRACTION_CONFIG = ExtractionConfig.builder()
            .disableOcr(true)
            .forceOcr(false)
            .outputFormat("html")
            .build();

    @Override
    public String convertToHtml(DocumentUpload upload) {
        String mimeType = resolveMimeType(upload);

        try {
            ExtractionResult result = Kreuzberg.extractBytes(upload.content(), mimeType, EXTRACTION_CONFIG);

            if (result == null) {
                throw new DocumentConversionFailedException("Kreuzberg-Konvertierung ist fehlgeschlagen.");
            }

            return normalize(result.getContent());
        } catch (KreuzbergException exception) {
            throw new DocumentConversionFailedException("Kreuzberg-Konvertierung ist fehlgeschlagen.", exception);
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
