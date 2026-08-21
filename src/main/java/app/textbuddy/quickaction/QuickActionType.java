package app.textbuddy.quickaction;

import java.util.Arrays;
import java.util.Optional;

public enum QuickActionType {
    PLAIN_LANGUAGE("plain-language"),
    BULLET_POINTS("bullet-points"),
    PROOFREAD("proofread"),
    SUMMARIZE("summarize"),
    FORMALITY("formality"),
    SOCIAL_MEDIA("social-media"),
    MEDIUM("medium"),
    CHARACTER_SPEECH("character-speech"),
    CUSTOM("custom");

    private final String path;

    QuickActionType(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static Optional<QuickActionType> fromPath(String value) {
        return Arrays.stream(values())
                .filter(action -> action.path.equals(value))
                .findFirst();
    }
}
