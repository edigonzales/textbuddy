package app.textbuddy.document;

public interface DocumentConversionService {
    DocumentConversionResponse convert(DocumentUpload upload, String ocrLanguage);
}
