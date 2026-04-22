package app.textbuddy.web.page;

import app.textbuddy.document.DocumentImportFormatCatalog;
import app.textbuddy.web.advisor.AdvisorRoleAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AdvisorRoleAccessService advisorRoleAccessService;
    private final DocumentImportFormatCatalog documentImportFormatCatalog;
    private final HomeAuthSupport homeAuthSupport;

    public HomeController(
            AdvisorRoleAccessService advisorRoleAccessService,
            DocumentImportFormatCatalog documentImportFormatCatalog,
            HomeAuthSupport homeAuthSupport
    ) {
        this.advisorRoleAccessService = advisorRoleAccessService;
        this.documentImportFormatCatalog = documentImportFormatCatalog;
        this.homeAuthSupport = homeAuthSupport;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        model.addAttribute("page", HomePageModel.defaultPage(
                homeAuthSupport.resolve(authentication),
                advisorRoleAccessService.listDocuments(authentication),
                documentImportFormatCatalog.labels(),
                documentImportFormatCatalog.acceptAttribute()
        ));
        return "pages/home";
    }
}
