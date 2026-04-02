package app.textbuddy.web.sentencerewrite;

import app.textbuddy.sentencerewrite.SentenceRewriteRequest;
import app.textbuddy.sentencerewrite.SentenceRewriteResponse;
import app.textbuddy.sentencerewrite.SentenceRewriteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sentence-rewrite")
public class SentenceRewriteController {

    private final SentenceRewriteService sentenceRewriteService;

    public SentenceRewriteController(SentenceRewriteService sentenceRewriteService) {
        this.sentenceRewriteService = sentenceRewriteService;
    }

    @PostMapping
    public SentenceRewriteResponse rewrite(@RequestBody SentenceRewriteRequest request) {
        return sentenceRewriteService.rewrite(request);
    }
}
