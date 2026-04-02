package app.textbuddy.web.wordsynonym;

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
class WordSynonymControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void postWordSynonymReturnsContextualSynonyms() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/word-synonym")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "word": "holprig",
                                  "context": "Dieser Satz ist etwas holprig."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.synonyms[0]").value("hakelig"))
                .andExpect(jsonPath("$.synonyms[1]").value("unrund"))
                .andExpect(jsonPath("$.synonyms[2]").value("stockend"));
    }
}
