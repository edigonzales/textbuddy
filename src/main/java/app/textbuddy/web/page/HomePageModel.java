package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 05 verbindet Wortfokus, Satzfokus und eine gemeinsame Rewrite-Bubble fuer kontextbezogene Synonyme und Satzalternativen."
        );
    }
}
