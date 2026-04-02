package app.textbuddy.web.textcorrection;

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
class TextCorrectionControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void postTextCorrectionReturnsCorrectionBlocks() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/text-correction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "This is teh text.",
                                  "language": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.original").value("This is teh text."))
                .andExpect(jsonPath("$.blocks[0].offset").value(8))
                .andExpect(jsonPath("$.blocks[0].length").value(3))
                .andExpect(jsonPath("$.blocks[0].shortMessage").value("Spelling"))
                .andExpect(jsonPath("$.blocks[0].replacements[0]").value("the"));
    }
}
