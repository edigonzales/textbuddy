package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 03 kombiniert Sprachauswahl, lokales Woerterbuch und inkrementelle Segmentpruefung auf dem bestehenden Textkorrektur-Endpoint."
        );
    }
}
