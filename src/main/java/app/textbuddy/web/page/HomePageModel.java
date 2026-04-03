package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 10 erweitert die Rewrite-Toolbar um Formality mit den Varianten formal und informal und nutzt weiter dieselbe SSE-, Diff- und Undo-Infrastruktur."
        );
    }
}
