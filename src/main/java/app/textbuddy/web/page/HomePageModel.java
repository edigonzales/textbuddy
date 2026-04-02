package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 04 verbindet Satzfokus, Bubble-Menue und alternative Formulierungen ueber einen eigenen Sentence-Rewrite-Endpoint."
        );
    }
}
