package app.textbuddy.document;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EditorFriendlyHtmlPostProcessorTest {

    private final EditorFriendlyHtmlPostProcessor postProcessor = new EditorFriendlyHtmlPostProcessor();

    @Test
    void stripsUnsafeMarkupAndNormalizesInlineRootContent() {
        String source = """
                <html>
                  <body>
                    <script>alert('x')</script>
                    <div class="outer">Einleitung <a href="javascript:alert(1)">Link</a></div>
                  </body>
                </html>
                """;

        String html = postProcessor.postProcess(source);

        assertThat(html).contains("Einleitung <a>Link</a>");
        assertThat(html).contains("<p>");
        assertThat(html).doesNotContain("script");
        assertThat(html).doesNotContain("javascript:");
    }

    @Test
    void keepsEditorRelevantBlockMarkup() {
        String source = "<h1>Titel</h1><ul><li>Punkt</li></ul><p>Absatz</p>";

        String html = postProcessor.postProcess(source);

        assertThat(html).contains("<h1>Titel</h1>");
        assertThat(html).contains("<ul>");
        assertThat(html).contains("<li>Punkt</li>");
        assertThat(html).contains("</ul>");
        assertThat(html).contains("<p>Absatz</p>");
    }

    @Test
    void returnsEmptyParagraphForBlankInput() {
        assertThat(postProcessor.postProcess("   ")).isEqualTo("<p></p>");
    }
}
