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
                .andExpect(content().string(containsString("data-testid=\"quick-action-panel\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-status\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-plain-language\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-bullet-points\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-proofread\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-summarize\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-summarize-option\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-formality\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-formality-option\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-social-media\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-social-media-option\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-medium\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-medium-option\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-character-speech\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-character-speech-option\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-custom\"")))
                .andExpect(content().string(containsString("data-testid=\"quick-action-custom-prompt\"")))
                .andExpect(content().string(containsString("data-testid=\"rewrite-diff-panel\"")))
                .andExpect(content().string(containsString("data-testid=\"rewrite-diff-undo\"")))
                .andExpect(content().string(containsString("data-testid=\"rewrite-bubble\"")))
                .andExpect(content().string(containsString("data-testid=\"rewrite-primary-action\"")))
                .andExpect(content().string(containsString("data-testid=\"rewrite-secondary-action\"")))
                .andExpect(content().string(containsString("data-testid=\"correction-panel\"")))
                .andExpect(content().string(containsString("data-testid=\"correction-status\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-panel\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-status\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-validate\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-doc-item\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-doc-checkbox\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-doc-open\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-results-panel\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-result-list\"")))
                .andExpect(content().string(containsString("data-testid=\"advisor-result-detail\"")))
                .andExpect(content().string(containsString("POST /api/advisor/validate")))
                .andExpect(content().string(containsString("Validation Stream")))
                .andExpect(content().string(containsString("Referenzdokumente")))
                .andExpect(content().string(containsString("Trefferliste")))
                .andExpect(content().string(containsString("Schreibweisungen")))
                .andExpect(content().string(containsString("/api/advisor/doc/schreibweisungen")))
                .andExpect(content().string(containsString("Quick Actions + Rewrite Bubble")))
                .andExpect(content().string(containsString("Plain Language")))
                .andExpect(content().string(containsString("Bullet Points")))
                .andExpect(content().string(containsString("Proofread")))
                .andExpect(content().string(containsString("Summarize")))
                .andExpect(content().string(containsString("Formality")))
                .andExpect(content().string(containsString("Social Media")))
                .andExpect(content().string(containsString("Medium")))
                .andExpect(content().string(containsString("Character Speech")))
                .andExpect(content().string(containsString("Custom")))
                .andExpect(content().string(containsString("Textkorrektur")))
                .andExpect(content().string(containsString("data-testid=\"correction-language\"")))
                .andExpect(content().string(containsString("data-testid=\"dictionary-form\"")))
                .andExpect(content().string(containsString("data-testid=\"dictionary-list\"")))
                .andExpect(content().string(containsString("Bubble-Menue")))
                .andExpect(content().string(containsString("Rewrite mit Streaming")));
    }
}
