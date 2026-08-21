package app.textbuddy.web.document;

import app.textbuddy.document.DocumentConversionResponse;
import app.textbuddy.document.DocumentConversionService;
import app.textbuddy.document.DocumentUpload;
import app.textbuddy.document.DocumentUploadTooLargeException;
import app.textbuddy.config.TextbuddyProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/convert/doc")
public class DocumentConversionController {

    private final DocumentConversionService documentConversionService;
    private final TextbuddyProperties.Document properties;

    public DocumentConversionController(
            DocumentConversionService documentConversionService,
            TextbuddyProperties textbuddyProperties
    ) {
        this.documentConversionService = documentConversionService;
        this.properties = textbuddyProperties.getDocument();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentConversionResponse convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "ocrLanguage", required = false) String ocrLanguage
    ) {
        if (file.getSize() > properties.normalizedMaxUploadSizeBytes()) {
            throw new DocumentUploadTooLargeException(
                    "Datei ist zu gross. Maximal erlaubt sind " + properties.describeMaxUploadSize() + "."
            );
        }

        try {
            return documentConversionService.convert(new DocumentUpload(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            ), ocrLanguage);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datei konnte nicht gelesen werden.", exception);
        }
    }
}
