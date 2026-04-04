package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 13 erweitert die Rewrite-Toolbar um Character Speech mit den Varianten direct_speech und indirect_speech und nutzt weiter dieselbe SSE-, Diff- und Undo-Infrastruktur."
        );
    }
}
