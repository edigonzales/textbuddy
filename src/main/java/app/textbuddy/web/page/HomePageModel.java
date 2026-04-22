package app.textbuddy.web.page;

import app.textbuddy.advisor.AdvisorDocsResponseItem;

import java.util.List;
import java.util.Map;

public record HomePageModel(
        String title,
        String subtitle,
        HomeAuthModel auth,
        List<AdvisorDocsResponseItem> advisorDocs,
        List<String> documentImportFormats,
        String documentImportAccept,
        String uiLocaleLanguage,
        String uiLocaleTag,
        List<String> supportedUiLanguages,
        String uiMessagesJson,
        Map<String, String> messages
) {
    public String t(String key) {
        if (messages == null) {
            return key;
        }
        return messages.getOrDefault(key, key);
    }
}
