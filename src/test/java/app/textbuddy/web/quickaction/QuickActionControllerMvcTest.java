package app.textbuddy.web.quickaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QuickActionControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rewritesPlainLanguageWithTheStub() throws Exception {
        mockMvc.perform(post("/api/quick-actions/plain-language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Der Fall ist kompliziert und relevant.","language":"de-CH"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("Kurz und einfach: Der Fall ist einfach und wichtig."));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            sentence|Kurzfassung:
            three_sentence|Kurzfassung in drei Saetzen:
            paragraph|Zusammenfassung:
            page|Zusammenfassung auf etwa einer Seite:
            management_summary|Management Summary
            """)
    void sendsEachSummaryOptionToTheMatchingStubVariant(String option, String expectedPrefix) throws Exception {
        mockMvc.perform(post("/api/quick-actions/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Der Gemeinderat bewilligt das Gesuch. Das Bauamt informiert die Gemeinde.",
                                 "language":"de-CH","option":"%s"}
                                """.formatted(option)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value(allOf(
                        startsWith(expectedPrefix),
                        containsString("Der Gemeinderat bewilligt das Gesuch.")
                )));
    }

    @Test
    void rejectsInvalidActionAndOption() throws Exception {
        String body = "{\"text\":\"Text\",\"language\":\"de\",\"option\":\"unbekannt\"}";

        mockMvc.perform(post("/api/quick-actions/unbekannt").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/quick-actions/summarize").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOversizedTextBeforeCallingTheAdapter() throws Exception {
        String oversizedText = "{\"text\":\"" + "x".repeat(50_001) + "\",\"language\":\"de\"}";

        mockMvc.perform(post("/api/quick-actions/plain-language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedText))
                .andExpect(status().isBadRequest());
    }
}
