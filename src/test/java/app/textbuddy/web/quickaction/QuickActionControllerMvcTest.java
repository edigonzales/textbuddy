package app.textbuddy.web.quickaction;

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
class QuickActionControllerMvcTest {

    @Autowired
    private WebApplicationContext context;

    @Test
    void returnsOneJsonResponseInsteadOfSse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(post("/api/quick-actions/plain-language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Der komplizierte Sachverhalt ist relevant.","language":"de-CH"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("Kurz und einfach: Der einfache Thema ist wichtig."));
    }

    @Test
    void rejectsInvalidActionAndOption() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        String body = "{\"text\":\"Text\",\"language\":\"de\",\"option\":\"unbekannt\"}";

        mockMvc.perform(post("/api/quick-actions/unbekannt").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/quick-actions/summarize").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOversizedTextAndCustomPromptBeforeCallingTheAdapter() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        String oversizedText = "{\"text\":\"" + "x".repeat(50_001) + "\",\"language\":\"de\"}";
        String oversizedPrompt = "{\"text\":\"Text\",\"language\":\"de\",\"prompt\":\""
                + "x".repeat(2_001) + "\"}";

        mockMvc.perform(post("/api/quick-actions/plain-language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedText))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/quick-actions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedPrompt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsValidRetryAndRejectsInvalidOrOversizedRetryData() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(post("/api/quick-actions/plain-language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text":"Der komplizierte Sachverhalt ist relevant.",
                                  "language":"de-CH",
                                  "previousText":"Der Text ist weiterhin kompliziert.",
                                  "previousFleschScore":47.3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").isString());

        mockMvc.perform(post("/api/quick-actions/plain-language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Text","language":"fr","previousText":"Entwurf","previousFleschScore":47.3}
                                """))
                .andExpect(status().isBadRequest());

        String oversizedCombinedText = "{\"text\":\"" + "x".repeat(25_001)
                + "\",\"language\":\"de-CH\",\"previousText\":\"" + "y".repeat(25_000)
                + "\",\"previousFleschScore\":47.3}";
        mockMvc.perform(post("/api/quick-actions/plain-language")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedCombinedText))
                .andExpect(status().isBadRequest());
    }
}
