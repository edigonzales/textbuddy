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
                "Phase 03 bringt lokale OCR für gescannte Dokumente, robustere Importgrenzen und editorfreundlich nachbearbeitetes HTML.",
                auth,
                List.copyOf(advisorDocs),
                List.copyOf(documentImportFormats),
                documentImportAccept
        );
    }
}
