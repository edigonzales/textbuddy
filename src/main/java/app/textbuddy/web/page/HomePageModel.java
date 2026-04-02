package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 00 stellt nur die sichtbare Shell, Stubs und den Frontend-Arbeitsbereich bereit."
        );
    }
}
