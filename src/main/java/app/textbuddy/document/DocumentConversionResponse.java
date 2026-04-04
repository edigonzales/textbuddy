package app.textbuddy.document;

import java.util.Objects;

public record DocumentConversionResponse(String html) {

    public DocumentConversionResponse {
        html = Objects.requireNonNullElse(html, "");
    }
}
