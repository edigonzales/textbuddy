package app.textbuddy.advisor;

import java.util.List;
import java.util.Optional;

public interface AdvisorCatalogService {

    List<AdvisorDocsResponseItem> listDocuments();

    Optional<AdvisorDocumentFile> getDocument(String name);
}
