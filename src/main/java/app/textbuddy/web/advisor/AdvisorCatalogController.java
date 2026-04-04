package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorCatalogService;
import app.textbuddy.advisor.AdvisorDocsResponseItem;
import app.textbuddy.advisor.AdvisorDocumentFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    private final AdvisorCatalogService advisorCatalogService;

    public AdvisorCatalogController(AdvisorCatalogService advisorCatalogService) {
        this.advisorCatalogService = advisorCatalogService;
    }

    @GetMapping("/docs")
    public List<AdvisorDocsResponseItem> listDocuments() {
        return advisorCatalogService.listDocuments();
    }

    @GetMapping("/doc/{name}")
    public ResponseEntity<Resource> getDocument(@PathVariable String name) {
        AdvisorDocumentFile document = advisorCatalogService.getDocument(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Advisor document not found: " + name
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
