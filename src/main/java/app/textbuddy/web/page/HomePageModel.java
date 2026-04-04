package app.textbuddy.web.page;

import app.textbuddy.advisor.AdvisorDocsResponseItem;

import java.util.List;

public record HomePageModel(
        String title,
        String subtitle,
        HomeAuthModel auth,
        List<AdvisorDocsResponseItem> advisorDocs,
        List<String> documentImportFormats,
        String documentImportAccept
) {

    public static HomePageModel defaultPage(
            HomeAuthModel auth,
            List<AdvisorDocsResponseItem> advisorDocs,
            List<String> documentImportFormats,
            String documentImportAccept
    ) {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 18 schliesst die Anwendung mit schaltbarer OIDC-Grundintegration, konsistenter Fehlerbehandlung, Logging-Politur und final aktivierten Kernfunktionen ab.",
                auth,
                List.copyOf(advisorDocs),
                List.copyOf(documentImportFormats),
                documentImportAccept
        );
    }
}
