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
                "Slice 15 macht den statischen Advisor-Katalog sichtbar, liefert PDF-Referenzen ueber Spring MVC aus und behaelt die bestehende Editor-, Rewrite- und Korrektur-Infrastruktur unveraendert bei.",
                List.copyOf(advisorDocs)
        );
    }
}
