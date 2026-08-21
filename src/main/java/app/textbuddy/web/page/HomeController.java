package app.textbuddy.web.page;

import app.textbuddy.document.DocumentImportFormatCatalog;
import app.textbuddy.advisor.AdvisorCatalog;
import app.textbuddy.web.i18n.UiMessageCatalog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class HomeController {

    private final AdvisorCatalog advisorCatalog;
    private final DocumentImportFormatCatalog documentImportFormatCatalog;
    private final HomeAuthSupport homeAuthSupport;
    private final UiMessageCatalog uiMessageCatalog;
    private final ObjectMapper objectMapper;

    public HomeController(
            AdvisorCatalog advisorCatalog,
            DocumentImportFormatCatalog documentImportFormatCatalog,
            HomeAuthSupport homeAuthSupport,
            UiMessageCatalog uiMessageCatalog,
            ObjectMapper objectMapper
    ) {
        this.advisorCatalog = advisorCatalog;
        this.documentImportFormatCatalog = documentImportFormatCatalog;
        this.homeAuthSupport = homeAuthSupport;
        this.uiMessageCatalog = uiMessageCatalog;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String home(
            Model model,
            Authentication authentication,
            Locale locale,
            HttpServletRequest request
    ) {
        Locale uiLocale = uiMessageCatalog.normalizeUiLocale(locale);
        Map<String, String> messages = uiMessageCatalog.resolve(uiLocale);
        HomeAuthModel auth = homeAuthSupport.resolve(authentication, (key) -> messages.getOrDefault(key, key));
        String uiMessagesJson = toJson(messages);
        List<String> supportedUiLanguages = uiMessageCatalog.supportedUiLanguages();
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        model.addAttribute("page", new HomePageModel(
                messages.getOrDefault("home.meta.title", "Textbuddy Workspace"),
                messages.getOrDefault("home.meta.subtitle", "Textbuddy"),
                auth,
                advisorCatalog.listDocuments(),
                documentImportFormatCatalog.labels(),
                documentImportFormatCatalog.acceptAttribute(),
                uiLocale.getLanguage(),
                uiLocale.toLanguageTag(),
                supportedUiLanguages,
                uiMessagesJson,
                csrfToken == null ? "" : csrfToken.getParameterName(),
                csrfToken == null ? "" : csrfToken.getHeaderName(),
                csrfToken == null ? "" : csrfToken.getToken(),
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
