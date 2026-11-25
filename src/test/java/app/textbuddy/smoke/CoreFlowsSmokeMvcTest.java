package app.textbuddy.smoke;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CoreFlowsSmokeMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeAndCoreJsonFlowsStayOperational() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Textbuddy")));

        mockMvc.perform(post("/api/text-correction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "This is teh text.",
                                  "language": "en-US"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[0].replacements[0]").value("the"));
    }

    @Test
    void quickActionsAndImportFlowsRemainOperational() throws Exception {
        mockMvc.perform(post("/api/quick-actions/plain-language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Der komplizierte Sachverhalt ist relevant.",
                                  "language": "de-DE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Kurz und einfach: Der einfache Thema ist wichtig."));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "import.md",
                "text/markdown",
                "# Import Titel\n\nErste Zeile".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/convert/doc").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value("<p># Import Titel</p><p>Erste Zeile</p>"));
    }
}
