package app.textbuddy.web.quickaction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CustomQuickActionControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void postCustomStreamReturnsSseChunksAndCompleteEvent() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        MvcResult mvcResult = mockMvc.perform(post("/api/quick-actions/custom/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Projektstart ist morgen.",
                                  "language": "de-DE",
                                  "prompt": "Formuliere den Text als interne Ankuendigung."
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult(1_000L);

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:chunk")))
                .andExpect(content().string(containsString("event:complete")))
                .andExpect(content().string(containsString("\"text\":\"Custom Rewrite")))
                .andExpect(content().string(containsString("Auftrag: Formuliere den Text als interne Ankuendigung.")))
                .andExpect(content().string(containsString("Projektstart ist morgen.")));
    }

    @Test
    void postCustomStreamRequiresAPrompt() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/quick-actions/custom/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Projektstart ist morgen.",
                                  "language": "de-DE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCustomStreamRejectsTooLongPrompts() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        String requestBody = """
                {
                  "text": "Projektstart ist morgen.",
                  "language": "de-DE",
                  "prompt": "%s"
                }
                """.formatted("x".repeat(401));

        mockMvc.perform(post("/api/quick-actions/custom/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
