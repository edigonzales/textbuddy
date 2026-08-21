package app.textbuddy.web.sentencerewrite;

import app.textbuddy.sentencerewrite.SentenceRewriteRequest;
import app.textbuddy.sentencerewrite.SentenceRewriteResponse;
import app.textbuddy.sentencerewrite.SentenceRewriteService;
import app.textbuddy.web.RequestInputValidator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sentence-rewrite")
public class SentenceRewriteController {

    private final SentenceRewriteService sentenceRewriteService;
    private final RequestInputValidator inputValidator;

    public SentenceRewriteController(SentenceRewriteService sentenceRewriteService, RequestInputValidator inputValidator) {
        this.sentenceRewriteService = sentenceRewriteService;
        this.inputValidator = inputValidator;
    }

    @PostMapping
    public SentenceRewriteResponse rewrite(@RequestBody SentenceRewriteRequest request) {
        inputValidator.text(request == null ? null : request.sentence());
        inputValidator.text(request == null ? null : request.context());
        return sentenceRewriteService.rewrite(request);
    }
}
