package app.textbuddy.web.advisor;

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
class AdvisorValidationControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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
                .andExpect(content().string(containsString("\"documentName\":\"empfehlungen-anglizismen-maerz-2020\"")))
                .andExpect(content().string(containsString("\"ruleId\":\"downloaden-statt-herunterladen\"")))
                .andExpect(content().string(containsString("\"matchedText\":\"downloaden\"")))
                .andExpect(content().string(containsString("\"documentName\":\"schreibweisungen\"")))
                .andExpect(content().string(containsString("\"ruleId\":\"per-sofort-vermeiden\"")))
                .andExpect(content().string(containsString("\"matchedText\":\"per sofort\"")));
    }
}
