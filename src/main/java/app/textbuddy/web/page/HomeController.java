package app.textbuddy.web.page;

import app.textbuddy.advisor.AdvisorCatalogService;
import app.textbuddy.document.DocumentImportFormatCatalog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AdvisorCatalogService advisorCatalogService;
    private final DocumentImportFormatCatalog documentImportFormatCatalog;

    public HomeController(
            AdvisorCatalogService advisorCatalogService,
            DocumentImportFormatCatalog documentImportFormatCatalog
    ) {
        this.advisorCatalogService = advisorCatalogService;
        this.documentImportFormatCatalog = documentImportFormatCatalog;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("page", HomePageModel.defaultPage(
                advisorCatalogService.listDocuments(),
                documentImportFormatCatalog.labels(),
                documentImportFormatCatalog.acceptAttribute()
        ));
        return "pages/home";
    }
}
