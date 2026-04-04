package app.textbuddy.integration.advisor;

import app.textbuddy.advisor.AdvisorDocument;
import app.textbuddy.advisor.AdvisorDocumentFile;
import app.textbuddy.advisor.AdvisorRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClasspathAdvisorDocumentRepositoryTest {

    @Test
    void findAllLoadsOrderedDocumentsAndResolvesPdfResources() {
        ClasspathAdvisorDocumentRepository repository = new ClasspathAdvisorDocumentRepository(
                new PathMatchingResourcePatternResolver(),
                new ObjectMapper()
        );

        List<AdvisorDocument> documents = repository.findAll();
        Optional<AdvisorDocumentFile> documentFile = repository.findDocument("schreibweisungen");

        assertThat(documents)
                .hasSize(5)
                .extracting(AdvisorDocument::name)
                .containsExactly(
                        "empfehlungen-anglizismen-maerz-2020",
                        "leitfaden_geschlechtergerechte_sprache_3aufl",
                        "rechtschreibleitfaden-2017",
                        "schreibweisungen",
                        "merkblatt_behoerdenbriefe"
                );

        assertThat(documents)
                .extracting(AdvisorDocument::pdfFileName)
                .containsExactly(
                        "empfehlungen-anglizismen-maerz-2020.pdf",
                        "leitfaden_geschlechtergerechte_sprache_3aufl.pdf",
                        "rechtschreibleitfaden-2017.pdf",
                        "schreibweisungen.pdf",
                        "merkblatt_behoerdenbriefe.pdf"
                );
        assertThat(documents)
                .extracting(document -> document.rules().size())
                .containsOnly(2);
        assertThat(documents.getFirst().rules())
                .extracting(AdvisorRule::id)
                .containsExactly("downloaden-statt-herunterladen", "meeting-und-feedback-pruefen");

        assertThat(documentFile).isPresent();
        assertThat(documentFile.orElseThrow().fileName()).isEqualTo("schreibweisungen.pdf");
        assertThat(documentFile.orElseThrow().resource().exists()).isTrue();
        assertThat(documentFile.orElseThrow().resource().isReadable()).isTrue();
    }
}
