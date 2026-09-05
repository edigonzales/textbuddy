package app.textbuddy.web.page;

import java.util.List;
import java.util.Map;

public record HomePageModel(
        String title,
        HomeAuthModel auth,
        List<String> documentImportFormats,
        String documentImportAccept,
        String uiLocaleLanguage,
        String uiLocaleTag,
        String uiMessagesJson,
        String csrfParameterName,
        String csrfHeaderName,
        String csrfToken,
        Map<String, String> messages
) {
    public String t(String key) {
        if (messages == null) {
            return key;
        }
        return messages.getOrDefault(key, key);
    }
}
