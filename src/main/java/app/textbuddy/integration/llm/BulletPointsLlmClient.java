package app.textbuddy.integration.llm;

import java.util.List;

public interface BulletPointsLlmClient {

    List<String> streamBulletPoints(String text, String language);
}
