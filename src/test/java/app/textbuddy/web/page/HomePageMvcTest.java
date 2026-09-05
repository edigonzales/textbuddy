package app.textbuddy.web.page;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
class HomePageMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void getRootRendersCurrentEditorShell() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/home"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("data-testid=\"skip-link\"")))
                .andExpect(content().string(containsString("data-auth-enabled=\"false\"")))
                .andExpect(content().string(containsString("data-testid=\"editor-island-root\"")))
                .andExpect(content().string(containsString("data-testid=\"document-import-input\"")))
                .andExpect(content().string(containsString("accept=\".pdf,.docx,.pptx,.xlsx,.html,.md,.adoc,.txt,.png,.jpg,.jpeg,.tif,.tiff\"")))
                .andExpect(content().string(containsString("/editor/editor-island.css")))
                .andExpect(content().string(containsString("/editor/editor-island.js")));
    }

    @Test
    void getRootSupportsUiLanguageSwitchViaQueryParameter() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/?lang=en"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/home"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<html lang=\"en\">")))
                .andExpect(content().string(containsString("Textbuddy Workspace")))
                .andExpect(content().string(containsString("Sign in with OIDC")))
                .andExpect(cookie().value("textbuddy-ui-locale", "en"));
    }
}
