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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                .andExpect(content().string(containsString("event:progress")))
                .andExpect(content().string(containsString("event:complete")))
                .andExpect(content().string(containsString("\"truncated\":false")))
                .andExpect(content().string(containsString("\"checked\":3")))
                .andExpect(content().string(containsString("\"checked\":4")))
                .andExpect(content().string(containsString("\"documentName\":\"empfehlungen-anglizismen-maerz-2020\"")))
                .andExpect(content().string(containsString("\"ruleId\":\"downloaden-statt-herunterladen\"")))
                .andExpect(content().string(containsString("\"matchedText\":\"downloaden\"")))
                .andExpect(content().string(containsString("\"start\":6")))
                .andExpect(content().string(containsString("\"end\":16")))
                .andExpect(content().string(containsString("\"documentName\":\"schreibweisungen\"")))
                .andExpect(content().string(containsString("\"ruleId\":\"per-sofort-vermeiden\"")))
                .andExpect(content().string(containsString("\"matchedText\":\"per sofort\"")));
    }

    @Test
    void postAdvisorFixReturnsOnlySelectedReplacements() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/advisor/fix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "  Das gilt per sofort.  ",
                                  "findings": [{
                                    "documentName": "schreibweisungen",
                                    "ruleId": "per-sofort-vermeiden",
                                    "start": 11,
                                    "end": 21,
                                    "matchedText": "per sofort",
                                    "suggestion": "ab sofort"
                                  }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("ab sofort"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void postAdvisorFixRejectsUnknownOrMismatchedFindings() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(post("/api/advisor/fix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "Das gilt per sofort.",
                                  "findings": [{
                                    "documentName": "schreibweisungen",
                                    "ruleId": "per-sofort-vermeiden",
                                    "start": 0,
                                    "end": 3,
                                    "matchedText": "per sofort",
                                    "suggestion": "ab sofort"
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
