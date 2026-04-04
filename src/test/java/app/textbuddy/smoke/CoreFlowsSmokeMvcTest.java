package app.textbuddy.smoke;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
                .andExpect(jsonPath("$.blocks[0].shortMessage").value("Spelling"));

        mockMvc.perform(post("/api/word-synonym")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "word": "holprig",
                                  "context": "Dieser Satz ist etwas holprig."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synonyms[0]").value("hakelig"));

        mockMvc.perform(post("/api/sentence-rewrite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sentence": "Dieser Satz ist etwas holprig."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alternatives[0]").value(containsString("Kurz gesagt")));
    }

    @Test
    void streamingAndImportFlowsRemainOperational() throws Exception {
        MvcResult streamResult = mockMvc.perform(post("/api/quick-actions/plain-language/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Der komplizierte Sachverhalt ist relevant.",
                                  "language": "de-DE"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        streamResult.getAsyncResult(1_000L);

        mockMvc.perform(asyncDispatch(streamResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:complete")));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "import.md",
                "text/markdown",
                "# Import Titel\n\nErste Zeile".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/convert/doc").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.html").value(containsString("<h1>Import Titel</h1>")));
    }
}
