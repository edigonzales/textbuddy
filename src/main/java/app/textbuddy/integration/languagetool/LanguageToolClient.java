package app.textbuddy.integration.languagetool;

import java.util.List;

public interface LanguageToolClient {

    List<LanguageToolMatch> check(String text, String language);
}
