package app.textbuddy.web.page;

import app.textbuddy.advisor.AdvisorCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AdvisorCatalogService advisorCatalogService;

    public HomeController(AdvisorCatalogService advisorCatalogService) {
        this.advisorCatalogService = advisorCatalogService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("page", HomePageModel.defaultPage(advisorCatalogService.listDocuments()));
        return "pages/home";
    }
}
