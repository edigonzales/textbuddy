package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 07 schaltet Bullet-Points-Streaming neben Plain Language frei und nutzt dieselbe SSE-, Diff- und Undo-Infrastruktur."
        );
    }
}
