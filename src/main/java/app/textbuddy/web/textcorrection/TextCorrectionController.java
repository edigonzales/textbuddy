package app.textbuddy.web.textcorrection;

import app.textbuddy.textcorrection.CorrectionRequest;
import app.textbuddy.textcorrection.CorrectionResponse;
import app.textbuddy.textcorrection.TextCorrectionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/text-correction")
public class TextCorrectionController {

    private final TextCorrectionService textCorrectionService;

    public TextCorrectionController(TextCorrectionService textCorrectionService) {
        this.textCorrectionService = textCorrectionService;
    }

    @PostMapping
    public CorrectionResponse correct(@RequestBody CorrectionRequest request) {
        return textCorrectionService.correct(request);
    }
}
