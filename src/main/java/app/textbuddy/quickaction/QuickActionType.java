package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Optional;

public enum QuickActionType {
    PLAIN_LANGUAGE("plain-language"),
    SUMMARIZE("summarize");

    private final String path;

    QuickActionType(String path) {
        this.path = path;
    }

    public static Optional<QuickActionType> fromPath(String value) {
        return Arrays.stream(values())
                .filter(action -> action.path.equals(value))
                .findFirst();
    }
}
