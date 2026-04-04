package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentUpload;

public interface DoclingClient {
    String convertToHtml(DocumentUpload upload);
}
