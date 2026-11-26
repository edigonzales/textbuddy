package app.textbuddy.advisor;

import org.springframework.core.io.Resource;

public record AdvisorDocumentFile(
        String fileName,
        Resource resource
) {
}
