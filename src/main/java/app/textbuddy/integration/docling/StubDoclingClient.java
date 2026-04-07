package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentUpload;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class StubDoclingClient implements DoclingClient {

    @Override
    public String convertToHtml(DocumentUpload upload, String ocrLanguage) {
        String extension = extractExtension(upload.filename());
        String text = new String(upload.content(), StandardCharsets.UTF_8);

        return switch (extension) {
            case "html" -> text.isBlank() ? "<p></p>" : text;
            case "md" -> markdownToHtml(text);
            case "adoc" -> asciidocToHtml(text);
            case "txt" -> plainTextToHtml(text);
            default -> binaryFallbackHtml(upload.filename(), upload.content().length);
        };
    }

    private static String markdownToHtml(String markdown) {
        return structuredTextToHtml(markdown, '#');
    }

    private static String asciidocToHtml(String asciidoc) {
        return structuredTextToHtml(asciidoc, '=');
    }

    private static String structuredTextToHtml(String value, char headingPrefix) {
        StringBuilder html = new StringBuilder();
        List<String> paragraphLines = new ArrayList<>();
        boolean inList = false;

        for (String rawLine : normalizeLines(value).split("\n", -1)) {
            String line = rawLine.strip();

            if (line.isEmpty()) {
                html.append(closeParagraph(paragraphLines));
                html.append(closeList(inList));
                inList = false;
                continue;
            }

            if (isHeading(line, headingPrefix)) {
                html.append(closeParagraph(paragraphLines));
                html.append(closeList(inList));
                inList = false;
                int level = Math.min(6, countLeadingCharacters(line, headingPrefix));
                String text = line.substring(level).trim();
                html.append("<h").append(level).append(">")
                        .append(escape(text))
                        .append("</h").append(level).append(">");
                continue;
            }

            if (line.startsWith("- ") || line.startsWith("* ")) {
                html.append(closeParagraph(paragraphLines));

                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }

                html.append("<li>")
                        .append(escape(line.substring(2).trim()))
                        .append("</li>");
                continue;
            }

            if (inList) {
                html.append("</ul>");
                inList = false;
            }

            paragraphLines.add(line);
        }

        html.append(closeParagraph(paragraphLines));
        html.append(closeList(inList));

        return html.isEmpty() ? "<p></p>" : html.toString();
    }

    private static String plainTextToHtml(String text) {
        String normalized = normalizeLines(text);

        if (normalized.isBlank()) {
            return "<p></p>";
        }

        return Arrays.stream(normalized.split("\n", -1))
                .map((line) -> line.isEmpty() ? "<p></p>" : "<p>" + escape(line) + "</p>")
                .reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append)
                .toString();
    }

    private static String binaryFallbackHtml(String filename, int sizeInBytes) {
        String safeFilename = escape(filename == null || filename.isBlank() ? "Dokument" : filename);

        return "<h1>" + safeFilename + "</h1>"
                + "<p>Stub-Import für binäre Dokumente. Für echte Konvertierung bitte einen Docling-Server konfigurieren.</p>"
                + "<p>Dateigrösse: " + sizeInBytes + " Bytes</p>";
    }

    private static String closeParagraph(List<String> paragraphLines) {
        if (paragraphLines.isEmpty()) {
            return "";
        }

        String paragraph = String.join(" ", paragraphLines);
        paragraphLines.clear();
        return "<p>" + escape(paragraph) + "</p>";
    }

    private static String closeList(boolean inList) {
        return inList ? "</ul>" : "";
    }

    private static boolean isHeading(String line, char prefix) {
        if (line.isEmpty() || line.charAt(0) != prefix || line.length() < 2) {
            return false;
        }

        int prefixCount = countLeadingCharacters(line, prefix);
        return line.length() > prefixCount && line.charAt(prefixCount) == ' ';
    }

    private static int countLeadingCharacters(String line, char value) {
        int count = 0;

        while (count < line.length() && line.charAt(count) == value) {
            count += 1;
        }

        return count;
    }

    private static String extractExtension(String filename) {
        String normalized = filename == null ? "" : filename.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.lastIndexOf('.');

        if (separator < 0 || separator == normalized.length() - 1) {
            return "";
        }

        return normalized.substring(separator + 1);
    }

    private static String normalizeLines(String value) {
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name());
    }
}
