package app.textbuddy.web.advisor;

import app.textbuddy.advisor.AdvisorDocsResponseItem;
import app.textbuddy.advisor.AdvisorDocumentFile;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorCatalogController {

    private final AdvisorRoleAccessService advisorRoleAccessService;

    public AdvisorCatalogController(AdvisorRoleAccessService advisorRoleAccessService) {
        this.advisorRoleAccessService = advisorRoleAccessService;
    }

    @GetMapping("/docs")
    public List<AdvisorDocsResponseItem> listDocuments(Authentication authentication) {
        return advisorRoleAccessService.listDocuments(authentication);
    }

    @GetMapping("/doc/{name}")
    public ResponseEntity<Resource> getDocument(@PathVariable String name, Authentication authentication) {
        AdvisorDocumentFile document = advisorRoleAccessService.requireDocument(name, authentication);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(document.fileName()).build().toString()
                )
                .body(document.resource());
    }
}
