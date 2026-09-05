package app.textbuddy.advisor;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AdvisorCatalogTest {

    @Test
    void loadsOrderedDocumentsOnceAndResolvesPdfResources() throws IOException {
        AdvisorCatalog catalog = new AdvisorCatalog(
                new PathMatchingResourcePatternResolver(),
                new ObjectMapper()
        );

        List<AdvisorDocument> documents = catalog.documents();
        Optional<AdvisorDocumentFile> documentFile = catalog.findDocument("schreibweisungen");

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
        assertThat(documents)
                .flatExtracting(AdvisorDocument::rules)
                .extracting(AdvisorRule::page)
                .containsOnly(1);

        assertThat(documentFile).isPresent();
        assertThat(documentFile.orElseThrow().fileName()).isEqualTo("schreibweisungen.pdf");
        Resource pdf = documentFile.orElseThrow().resource();
        assertThat(pdf.exists()).isTrue();
        assertThat(pdf.isReadable()).isTrue();
        assertThat(pdf.contentLength()).isGreaterThan(10_000L);
        byte[] pdfBytes = pdf.getContentAsByteArray();
        assertThat(new String(pdfBytes, 0, 8, StandardCharsets.US_ASCII)).isEqualTo("%PDF-1.4");
        assertThat(new String(pdfBytes, pdfBytes.length - 6, 6, StandardCharsets.US_ASCII))
                .contains("%%EOF");
    }
}
