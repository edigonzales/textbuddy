package app.textbuddy.advisor;

import org.springframework.core.io.Resource;

public record AdvisorDocumentFile(
        String name,
        String fileName,
        Resource resource
) {
}
