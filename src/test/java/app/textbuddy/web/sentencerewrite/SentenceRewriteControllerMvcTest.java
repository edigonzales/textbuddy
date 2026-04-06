package app.textbuddy.web.sentencerewrite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SentenceRewriteControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void postSentenceRewriteReturnsAlternatives() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/sentence-rewrite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sentence": "Dieser Satz ist etwas holprig."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.original").value("Dieser Satz ist etwas holprig."))
                .andExpect(jsonPath("$.alternatives[0]").value("Kurz gesagt: Dieser Satz ist etwas holprig."))
                .andExpect(jsonPath("$.alternatives[1]").value("Anders formuliert: Dieser Satz ist etwas holprig."))
                .andExpect(jsonPath("$.alternatives[2]").value("Praeziser gesagt: Dieser Satz ist etwas holprig."));
    }

    @Test
    void postSentenceRewriteAcceptsOptionalContext() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/sentence-rewrite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sentence": "Dieser Satz ist etwas holprig.",
                                  "context": "Der Absatz erklärt die Ausgangslage und enthält zusätzliche Nuancen."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.original").value("Dieser Satz ist etwas holprig."));
    }
}
