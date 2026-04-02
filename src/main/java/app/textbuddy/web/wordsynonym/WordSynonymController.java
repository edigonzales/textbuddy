package app.textbuddy.web.wordsynonym;

import app.textbuddy.wordsynonym.WordSynonymRequest;
import app.textbuddy.wordsynonym.WordSynonymResponse;
import app.textbuddy.wordsynonym.WordSynonymService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/word-synonym")
public class WordSynonymController {

    private final WordSynonymService wordSynonymService;

    public WordSynonymController(WordSynonymService wordSynonymService) {
        this.wordSynonymService = wordSynonymService;
    }

    @PostMapping
    public WordSynonymResponse synonyms(@RequestBody WordSynonymRequest request) {
        return wordSynonymService.synonyms(request);
    }
}
