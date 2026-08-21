package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorDocsResponseItem;
import app.textbuddy.advisor.AdvisorDocumentFile;
import app.textbuddy.advisor.AdvisorCatalog;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorCatalogController {

    private final AdvisorCatalog advisorCatalog;

    public AdvisorCatalogController(AdvisorCatalog advisorCatalog) {
        this.advisorCatalog = advisorCatalog;
    }

    @GetMapping("/docs")
    public List<AdvisorDocsResponseItem> listDocuments() {
        return advisorCatalog.listDocuments();
    }

    @GetMapping("/doc/{name}")
    public ResponseEntity<Resource> getDocument(@PathVariable String name) {
        AdvisorDocumentFile document = advisorCatalog.findDocument(name)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Advisor-Dokument wurde nicht gefunden."
                ));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(document.fileName()).build().toString()
                )
                .body(document.resource());
    }
}
