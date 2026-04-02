package app.textbuddy.web.page;

public record HomePageModel(String title, String subtitle) {

    public static HomePageModel defaultPage() {
        return new HomePageModel(
                "Textbuddy Workspace",
                "Slice 06 ergaenzt eine Quick-Action-Infrastruktur mit Plain-Language-SSE, gemeinsamem Stream-Handling sowie Diff und komplettem Rewrite-Undo."
        );
    }
}
