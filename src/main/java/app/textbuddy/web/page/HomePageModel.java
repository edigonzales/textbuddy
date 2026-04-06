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
                "Phase 02 bringt Frontend-Parität mit erweiterten Korrektursprachen, eingebettetem Advisor-PDF-Viewer und produktiver Textstatistik.",
                auth,
                List.copyOf(advisorDocs),
                List.copyOf(documentImportFormats),
                documentImportAccept
        );
    }
}
