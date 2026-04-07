package app.textbuddy.web.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "textbuddy.document.mode=stub",
        "textbuddy.document.max-upload-size=10B",
        "spring.servlet.multipart.max-file-size=10B",
        "spring.servlet.multipart.max-request-size=10B"
})
@AutoConfigureMockMvc
class DocumentConversionUploadLimitsMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsTooLargeUploadWithControlledProblemJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "gross.txt",
                "text/plain",
                "12345678901".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/convert/doc").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("Datei ist zu gross")))
                .andExpect(jsonPath("$.path").value("/api/convert/doc"));
    }
}
