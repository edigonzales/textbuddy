package app.textbuddy.web.wordsynonym;

import app.textbuddy.wordsynonym.WordSynonymRequest;
import app.textbuddy.wordsynonym.WordSynonymResponse;
import app.textbuddy.wordsynonym.WordSynonymService;
import app.textbuddy.web.RequestInputValidator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/word-synonym")
public class WordSynonymController {

    private final WordSynonymService wordSynonymService;
    private final RequestInputValidator inputValidator;

    public WordSynonymController(WordSynonymService wordSynonymService, RequestInputValidator inputValidator) {
        this.wordSynonymService = wordSynonymService;
        this.inputValidator = inputValidator;
    }

    @PostMapping
    public WordSynonymResponse synonyms(@RequestBody WordSynonymRequest request) {
        inputValidator.prompt(request == null ? null : request.word());
        inputValidator.text(request == null ? null : request.context());
        return wordSynonymService.synonyms(request);
    }
}
