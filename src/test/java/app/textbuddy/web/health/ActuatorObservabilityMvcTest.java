package app.textbuddy.web.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorObservabilityMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReportsAdapterComponents() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.llm.status").value("UP"))
                .andExpect(jsonPath("$.components.languagetool.status").value("UP"))
                .andExpect(jsonPath("$.components.documentImport.status").value("UP"));
    }

    @Test
    void infoEndpointIncludesOperationalModes() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textbuddy.authEnabled").value(false))
                .andExpect(jsonPath("$.textbuddy.llmMode").value("stub"))
                .andExpect(jsonPath("$.textbuddy.languageToolMode").value("stub"))
                .andExpect(jsonPath("$.textbuddy.documentImportMode").value("stub"))
                .andExpect(jsonPath("$.textbuddy.runtimeInitializationEnabled").value(true))
                .andExpect(jsonPath("$.textbuddy.runtimeHome").value(containsString("build/test-runtime")));
    }
}
