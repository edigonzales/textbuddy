package app.textbuddy.document;

import java.util.Objects;

public record DocumentUpload(
        String filename,
        String contentType,
        byte[] content
) {

    public DocumentUpload {
        filename = Objects.requireNonNullElse(filename, "");
        contentType = Objects.requireNonNullElse(contentType, "");
        content = content == null ? new byte[0] : content;
    }

    public int size() {
        return content.length;
    }
}
