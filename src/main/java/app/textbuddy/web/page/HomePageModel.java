package app.textbuddy.web.page;

import app.textbuddy.advisor.AdvisorDocsResponseItem;

import java.util.List;

public record HomePageModel(
        String title,
        String subtitle,
        List<AdvisorDocsResponseItem> advisorDocs,
        List<String> documentImportFormats,
        String documentImportAccept
) {

    public static HomePageModel defaultPage(
            List<AdvisorDocsResponseItem> advisorDocs,
            List<String> documentImportFormats,
            String documentImportAccept
    ) {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 17 importiert Dokumente jetzt per Upload oder Drag-and-Drop, konvertiert sie ueber Docling nach HTML und uebernimmt das Ergebnis direkt in die bestehende Editor-Insel.",
                List.copyOf(advisorDocs),
                List.copyOf(documentImportFormats),
                documentImportAccept
        );
    }
}
