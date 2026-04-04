package app.textbuddy.document;

import java.util.Arrays;
import java.util.Objects;

public record DocumentUpload(
        String filename,
        String contentType,
        byte[] content
) {

    public DocumentUpload {
        filename = Objects.requireNonNullElse(filename, "");
        contentType = Objects.requireNonNullElse(contentType, "");
        content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
