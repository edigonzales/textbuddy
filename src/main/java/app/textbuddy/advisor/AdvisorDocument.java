package app.textbuddy.advisor;

import java.util.List;

public record AdvisorDocument(
        int order,
        String name,
        String title,
        String summary,
        String source,
        List<String> allowedRoles,
        String pdfFileName,
        List<AdvisorRule> rules
) {
}
