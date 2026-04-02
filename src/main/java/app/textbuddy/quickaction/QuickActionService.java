package app.textbuddy.quickaction;

public interface QuickActionService {

    void streamPlainLanguage(QuickActionStreamRequest request, QuickActionStreamHandler handler);
}
