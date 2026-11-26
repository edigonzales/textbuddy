package app.textbuddy.web.advisor;

import app.textbuddy.integration.llm.LlmProviderException;
import app.textbuddy.integration.llm.StubTextbuddyLlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdvisorValidationControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoSpyBean
    private StubTextbuddyLlmClient llmClient;

    @Test
    void postAdvisorValidateReturnsValidationEvents() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        MvcResult mvcResult = mockMvc.perform(post("/api/advisor/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Bitte downloaden Sie das Formular per sofort.",
                                  "docs": [
                                    "empfehlungen-anglizismen-maerz-2020",
                                    "schreibweisungen"
                                  ]
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvcResult.getAsyncResult(1_000L);

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:validation")))
                .andExpect(content().string(containsString("event:progress")))
                .andExpect(content().string(containsString("event:complete")))
                .andExpect(content().string(containsString("\"incomplete\":false")))
                .andExpect(content().string(containsString("\"checked\":3")))
                .andExpect(content().string(containsString("\"checked\":4")))
                .andExpect(content().string(containsString("\"matchedText\":\"downloaden\"")))
                .andExpect(content().string(containsString("\"replacement\":\"herunterladen\"")))
                .andExpect(content().string(containsString("\"referenceUrl\":\"/api/advisor/doc/empfehlungen-anglizismen-maerz-2020#page=")))
                .andExpect(content().string(containsString("\"start\":6")))
                .andExpect(content().string(containsString("\"end\":16")))
                .andExpect(content().string(containsString("\"matchedText\":\"per sofort\"")));
    }

    @Test
    void removedFixEndpointReturnsNotFound() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/advisor/fix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void providerFailureEndsTheStreamWithASafeErrorAndNoCompletion() throws Exception {
        doThrow(new LlmProviderException("Synthetisches Provider-Detail"))
                .when(llmClient).validate(eq("Bitte lesen."), anyList());
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        MvcResult result = mockMvc.perform(post("/api/advisor/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"Bitte lesen.","docs":["schreibweisungen"]}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();
        result.getAsyncResult(1_000L);

        String stream = mockMvc.perform(asyncDispatch(result)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(stream).contains("event:error", "Richtlinienprüfung fehlgeschlagen.")
                .doesNotContain("Synthetisches Provider-Detail", "event:complete");
    }
}
