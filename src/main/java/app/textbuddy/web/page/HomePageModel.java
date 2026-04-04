package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 14 erweitert die Rewrite-Toolbar um eine freie Custom Quick Action mit Pflicht-Prompt und nutzt weiter dieselbe SSE-, Diff- und Undo-Infrastruktur."
        );
    }
}
