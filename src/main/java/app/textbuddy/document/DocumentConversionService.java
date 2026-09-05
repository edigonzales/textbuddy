package app.textbuddy.document;

import app.textbuddy.config.TextbuddyProperties;
import app.textbuddy.integration.docling.DoclingClient;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DocumentConversionService {

    private static final String EMPTY_UPLOAD_MESSAGE = "Bitte eine nicht leere Datei hochladen.";
    private static final String LARGE_UPLOAD_MESSAGE_PREFIX = "Datei ist zu gross. Maximal erlaubt sind ";
    private static final String LARGE_UPLOAD_MESSAGE_SUFFIX = ".";

    private final DoclingClient doclingClient;
    private final DocumentImportFormatCatalog formatCatalog;
    private final TextbuddyProperties.Document properties;
    private final EditorFriendlyHtmlPostProcessor htmlPostProcessor;

    public DocumentConversionService(
            DoclingClient doclingClient,
            DocumentImportFormatCatalog formatCatalog,
            TextbuddyProperties textbuddyProperties,
            EditorFriendlyHtmlPostProcessor htmlPostProcessor
    ) {
        this.doclingClient = doclingClient;
        this.formatCatalog = formatCatalog;
        this.properties = textbuddyProperties.getDocument();
        this.htmlPostProcessor = htmlPostProcessor;
    }

    public DocumentConversionResponse convert(DocumentUpload upload, String ocrLanguage) {
        DocumentUpload normalizedUpload = upload == null
                ? new DocumentUpload("", "", new byte[0])
                : upload;

        if (normalizedUpload.size() == 0) {
            throw new EmptyDocumentUploadException(EMPTY_UPLOAD_MESSAGE);
        }

        if (normalizedUpload.size() > properties.normalizedMaxUploadSizeBytes()) {
            throw new DocumentUploadTooLargeException(
                    LARGE_UPLOAD_MESSAGE_PREFIX
                            + properties.describeMaxUploadSize()
                            + LARGE_UPLOAD_MESSAGE_SUFFIX
            );
        }

        if (!formatCatalog.supports(normalizedUpload.filename(), normalizedUpload.contentType())) {
            throw new UnsupportedDocumentFormatException(
                    "Nicht unterstütztes Dateiformat. Unterstützt werden: "
                            + formatCatalog.describeSupportedFormats()
                            + "."
            );
        }

        String normalizedOcrLanguage = DocumentOcrLanguage.normalizeCode(ocrLanguage);
        String html = Objects.requireNonNullElse(
                doclingClient.convertToHtml(normalizedUpload, normalizedOcrLanguage),
                ""
        );

        if (html.isBlank()) {
            throw new DocumentConversionFailedException("Dokumentimport hat keinen Inhalt geliefert.");
        }

        String editorFriendlyHtml = htmlPostProcessor.postProcess(html);

        if (editorFriendlyHtml.isBlank()) {
            throw new DocumentConversionFailedException("Dokumentimport hat kein HTML für dieses Dokument geliefert.");
        }

        return new DocumentConversionResponse(editorFriendlyHtml);
    }
}
