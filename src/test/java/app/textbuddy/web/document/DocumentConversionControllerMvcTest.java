package app.textbuddy.web.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DocumentConversionControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void postConvertDocReturnsHtmlForSupportedFiles() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "import.md",
                "text/markdown",
                "# Import Titel\n\nErste Zeile".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/convert/doc").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.html").value(containsString("<h1>Import Titel</h1>")))
                .andExpect(jsonPath("$.html").value(containsString("<p>Erste Zeile</p>")));
    }

    @Test
    void postConvertDocRejectsUnsupportedFiles() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.exe",
                "application/octet-stream",
                "noop".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/convert/doc").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("Nicht unterstütztes Dateiformat.")))
                .andExpect(jsonPath("$.path").value("/api/convert/doc"))
                .andExpect(jsonPath("$.traceId").isString());
    }
}
