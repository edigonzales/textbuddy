package app.textbuddy.web.page;

import app.textbuddy.advisor.AdvisorDocsResponseItem;

import java.util.List;

public record HomePageModel(
        String title,
        String subtitle,
        List<AdvisorDocsResponseItem> advisorDocs
) {

    public static HomePageModel defaultPage(List<AdvisorDocsResponseItem> advisorDocs) {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 16 validiert Editor-Texte jetzt gegen statische Advisor-Regeln, streamt Treffer ueber SseEmitter und behaelt die bestehende Editor-, Rewrite- und Korrektur-Infrastruktur bei.",
                List.copyOf(advisorDocs)
        );
    }
}
