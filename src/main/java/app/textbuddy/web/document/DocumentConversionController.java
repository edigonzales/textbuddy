package app.textbuddy.web.document;

import app.textbuddy.document.DocumentConversionResponse;
import app.textbuddy.document.DocumentConversionService;
import app.textbuddy.document.DocumentUpload;
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

    public DocumentConversionController(DocumentConversionService documentConversionService) {
        this.documentConversionService = documentConversionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentConversionResponse convert(@RequestParam("file") MultipartFile file) {
        try {
            return documentConversionService.convert(new DocumentUpload(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            ));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datei konnte nicht gelesen werden.", exception);
        }
    }
}
