package app.textbuddy.web.advisor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdvisorCatalogControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void getAdvisorDocsReturnsStaticCatalog() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/advisor/docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].name").value("empfehlungen-anglizismen-maerz-2020"))
                .andExpect(jsonPath("$[0].title").value("Empfehlungen zu Anglizismen"))
                .andExpect(jsonPath("$[0].documentUrl").value("/api/advisor/doc/empfehlungen-anglizismen-maerz-2020"))
                .andExpect(jsonPath("$[3].name").value("schreibweisungen"));
    }

    @Test
    void getAdvisorDocumentReturnsPdfInline() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/api/advisor/doc/schreibweisungen"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("schreibweisungen.pdf")))
                .andExpect(content().bytes(org.springframework.util.StreamUtils.copyToByteArray(
                        webApplicationContext.getResource("classpath:advisor/docs/schreibweisungen.pdf").getInputStream()
                )));
    }
}
