package app.textbuddy.document;

import app.textbuddy.integration.docling.DoclingClient;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DefaultDocumentConversionService implements DocumentConversionService {

    private static final String EMPTY_UPLOAD_MESSAGE = "Bitte eine nicht leere Datei hochladen.";

    private final DoclingClient doclingClient;
    private final DocumentImportFormatCatalog formatCatalog;

    public DefaultDocumentConversionService(
            DoclingClient doclingClient,
            DocumentImportFormatCatalog formatCatalog
    ) {
        this.doclingClient = doclingClient;
        this.formatCatalog = formatCatalog;
    }

    @Override
    public DocumentConversionResponse convert(DocumentUpload upload) {
        DocumentUpload normalizedUpload = upload == null
                ? new DocumentUpload("", "", new byte[0])
                : upload;

        if (normalizedUpload.content().length == 0) {
            throw new EmptyDocumentUploadException(EMPTY_UPLOAD_MESSAGE);
        }

        if (!formatCatalog.supports(normalizedUpload.filename(), normalizedUpload.contentType())) {
            throw new UnsupportedDocumentFormatException(
                    "Nicht unterstuetztes Dateiformat. Unterstuetzt werden: "
                            + formatCatalog.describeSupportedFormats()
                            + "."
            );
        }

        String html = Objects.requireNonNullElse(doclingClient.convertToHtml(normalizedUpload), "");

        if (html.isBlank()) {
            throw new DocumentConversionFailedException("Docling hat kein HTML fuer dieses Dokument geliefert.");
        }

        return new DocumentConversionResponse(html);
    }
}
