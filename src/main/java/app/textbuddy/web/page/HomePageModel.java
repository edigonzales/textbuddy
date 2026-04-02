package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 01 aktiviert die lokale Editor-Insel mit Plain-Text-Bearbeitung, Mirror und Undo/Redo."
        );
    }
}
