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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
class HomePageMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void getRootRendersEditorIslandShell() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/home"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Textbuddy")))
                .andExpect(content().string(containsString("data-testid=\"editor-shell\"")))
                .andExpect(content().string(containsString("data-testid=\"editor-island-root\"")))
                .andExpect(content().string(containsString("data-testid=\"editor-toolbar\"")))
                .andExpect(content().string(containsString("data-testid=\"editor-mirror\"")))
                .andExpect(content().string(containsString("data-testid=\"editor-character-count\">0</strong>")))
                .andExpect(content().string(containsString("data-testid=\"editor-word-count\">0</strong>")))
                .andExpect(content().string(containsString("/editor/editor-island.css")))
                .andExpect(content().string(containsString("/editor/editor-island.js")))
                .andExpect(content().string(containsString("data-testid=\"rewrite-bubble\"")))
                .andExpect(content().string(containsString("data-testid=\"rewrite-primary-action\"")))
                .andExpect(content().string(containsString("data-testid=\"rewrite-secondary-action\"")))
                .andExpect(content().string(containsString("data-testid=\"correction-panel\"")))
                .andExpect(content().string(containsString("data-testid=\"correction-status\"")))
                .andExpect(content().string(containsString("Word Synonym + Sentence Rewrite")))
                .andExpect(content().string(containsString("POST /api/word-synonym")))
                .andExpect(content().string(containsString("POST /api/sentence-rewrite")))
                .andExpect(content().string(containsString("Textkorrektur")))
                .andExpect(content().string(containsString("data-testid=\"correction-language\"")))
                .andExpect(content().string(containsString("data-testid=\"dictionary-form\"")))
                .andExpect(content().string(containsString("data-testid=\"dictionary-list\"")))
                .andExpect(content().string(containsString("Bubble-Menue")));
    }
}
