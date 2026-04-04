package app.textbuddy.integration.advisor;

import app.textbuddy.advisor.AdvisorDocument;
import app.textbuddy.advisor.AdvisorDocumentFile;

import java.util.List;
import java.util.Optional;

public interface AdvisorDocumentRepository {

    List<AdvisorDocument> findAll();

    Optional<AdvisorDocumentFile> findDocument(String name);
}
