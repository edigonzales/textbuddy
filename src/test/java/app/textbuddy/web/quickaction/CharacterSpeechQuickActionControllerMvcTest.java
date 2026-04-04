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
class CharacterSpeechQuickActionControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void postCharacterSpeechStreamReturnsSseChunksAndCompleteEvent() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        MvcResult mvcResult = mockMvc.perform(post("/api/quick-actions/character-speech/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Projekt ist freigegeben. Team startet am Montag.",
                                  "language": "de-DE",
                                  "option": "direct_speech"
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
                .andExpect(content().string(containsString(
                        "\"text\":\"Direkte Rede\\n\\n\\\"Projekt ist freigegeben.\\\", sagte die Figur.\\n\\\"Team startet am Montag.\\\", antwortete die andere Figur.\""
                )));
    }

    @Test
    void postCharacterSpeechStreamRequiresAnOption() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/quick-actions/character-speech/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Projekt ist freigegeben.",
                                  "language": "de-DE"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCharacterSpeechStreamRejectsUnknownOptions() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/quick-actions/character-speech/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Projekt ist freigegeben.",
                                  "language": "de-DE",
                                  "option": "narrator"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
