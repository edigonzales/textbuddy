package app.textbuddy.web.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ErrorHandlingMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingApiEndpointReturnsProblemJson() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Die angeforderte Ressource wurde nicht gefunden."))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void missingHtmlPageRendersErrorTemplate() throws Exception {
        mockMvc.perform(get("/does-not-exist").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Textbuddy Fehler")))
                .andExpect(content().string(containsString("404 Not Found")))
                .andExpect(content().string(containsString("Trace-ID")))
                .andExpect(content().string(containsString("Zur Startseite")));
    }
}
