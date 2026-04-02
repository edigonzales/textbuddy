package app.textbuddy.quickaction;

public interface QuickActionStreamHandler {

    void chunk(String text);

    void complete(String text);

    void error(String message);
}
