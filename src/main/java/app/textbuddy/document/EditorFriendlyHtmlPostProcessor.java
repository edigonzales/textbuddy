package app.textbuddy.document;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Set;

@Component
public class EditorFriendlyHtmlPostProcessor {

    private static final Set<String> ROOT_BLOCK_TAGS = Set.of(
            "p",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "ul",
            "ol",
            "blockquote",
            "pre"
    );

    private static final Safelist EDITOR_SAFE_LIST = Safelist.none()
            .addTags(
                    "p",
                    "br",
                    "h1",
                    "h2",
                    "h3",
                    "h4",
                    "h5",
                    "h6",
                    "ul",
                    "ol",
                    "li",
                    "blockquote",
                    "pre",
                    "code",
                    "strong",
                    "em",
                    "b",
                    "i",
                    "u",
                    "a"
            )
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https", "mailto");

    public String postProcess(String html) {
        String source = html == null ? "" : html.trim();

        if (source.isBlank()) {
            return "<p></p>";
        }

        Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(source, "", EDITOR_SAFE_LIST, outputSettings);
        Document document = Jsoup.parseBodyFragment(cleaned);
        document.outputSettings().prettyPrint(false);
        Element body = document.body();

        wrapRootInlineNodes(body);
        removeEmptyParagraphs(body);

        String normalized = body.html().trim();
        return normalized.isBlank() ? "<p></p>" : normalized;
    }

    private void wrapRootInlineNodes(Element body) {
        Element currentParagraph = null;

        for (Node node : new ArrayList<>(body.childNodes())) {
            if (node instanceof TextNode textNode) {
                if (textNode.text().trim().isEmpty()) {
                    node.remove();
                    continue;
                }

                if (currentParagraph == null) {
                    currentParagraph = new Element(Tag.valueOf("p"), "");
                    node.before(currentParagraph);
                }

                currentParagraph.appendChild(node);
                continue;
            }

            if (node instanceof Element element) {
                if (ROOT_BLOCK_TAGS.contains(element.tagName())) {
                    currentParagraph = null;
                    continue;
                }

                if (currentParagraph == null) {
                    currentParagraph = new Element(Tag.valueOf("p"), "");
                    node.before(currentParagraph);
                }

                currentParagraph.appendChild(node);
                continue;
            }

            node.remove();
        }
    }

    private void removeEmptyParagraphs(Element body) {
        body.select("p").removeIf((paragraph) ->
                paragraph.text().trim().isEmpty() && paragraph.children().isEmpty()
        );
    }
}
