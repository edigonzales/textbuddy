package app.textbuddy.web.page;

import app.textbuddy.document.DocumentImportFormatCatalog;
import app.textbuddy.web.i18n.UiMessageCatalog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.textbuddy.web.advisor.AdvisorRoleAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class HomeController {

    private final AdvisorRoleAccessService advisorRoleAccessService;
    private final DocumentImportFormatCatalog documentImportFormatCatalog;
    private final HomeAuthSupport homeAuthSupport;
    private final UiMessageCatalog uiMessageCatalog;
    private final ObjectMapper objectMapper;

    public HomeController(
            AdvisorRoleAccessService advisorRoleAccessService,
            DocumentImportFormatCatalog documentImportFormatCatalog,
            HomeAuthSupport homeAuthSupport,
            UiMessageCatalog uiMessageCatalog,
            ObjectMapper objectMapper
    ) {
        this.advisorRoleAccessService = advisorRoleAccessService;
        this.documentImportFormatCatalog = documentImportFormatCatalog;
        this.homeAuthSupport = homeAuthSupport;
        this.uiMessageCatalog = uiMessageCatalog;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication, Locale locale) {
        Locale uiLocale = uiMessageCatalog.normalizeUiLocale(locale);
        Map<String, String> messages = uiMessageCatalog.resolve(uiLocale);
        HomeAuthModel auth = homeAuthSupport.resolve(authentication, (key) -> messages.getOrDefault(key, key));
        String uiMessagesJson = toJson(messages);
        List<String> supportedUiLanguages = uiMessageCatalog.supportedUiLanguages();

        model.addAttribute("page", new HomePageModel(
                messages.getOrDefault("home.meta.title", "Textbuddy Workspace"),
                messages.getOrDefault("home.meta.subtitle", "Textbuddy"),
                auth,
                advisorRoleAccessService.listDocuments(authentication),
                documentImportFormatCatalog.labels(),
                documentImportFormatCatalog.acceptAttribute(),
                uiLocale.getLanguage(),
                uiLocale.toLanguageTag(),
                supportedUiLanguages,
                uiMessagesJson,
                messages
        ));
        return "pages/home";
    }

    private String toJson(Map<String, String> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
