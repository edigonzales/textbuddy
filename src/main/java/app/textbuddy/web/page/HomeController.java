package app.textbuddy.web.page;

import app.textbuddy.advisor.AdvisorCatalogService;
import app.textbuddy.document.DocumentImportFormatCatalog;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AdvisorCatalogService advisorCatalogService;
    private final DocumentImportFormatCatalog documentImportFormatCatalog;
    private final HomeAuthSupport homeAuthSupport;

    public HomeController(
            AdvisorCatalogService advisorCatalogService,
            DocumentImportFormatCatalog documentImportFormatCatalog,
            HomeAuthSupport homeAuthSupport
    ) {
        this.advisorCatalogService = advisorCatalogService;
        this.documentImportFormatCatalog = documentImportFormatCatalog;
        this.homeAuthSupport = homeAuthSupport;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        model.addAttribute("page", HomePageModel.defaultPage(
                homeAuthSupport.resolve(authentication),
                advisorCatalogService.listDocuments(),
                documentImportFormatCatalog.labels(),
                documentImportFormatCatalog.acceptAttribute()
        ));
        return "pages/home";
    }
}
