package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 09 erweitert die Rewrite-Toolbar um Summarize mit festen Varianten und nutzt weiter dieselbe SSE-, Diff- und Undo-Infrastruktur."
        );
    }
}
