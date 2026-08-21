package app.textbuddy.web.textcorrection;

import app.textbuddy.textcorrection.CorrectionRequest;
import app.textbuddy.textcorrection.CorrectionResponse;
import app.textbuddy.textcorrection.TextCorrectionService;
import app.textbuddy.web.RequestInputValidator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/text-correction")
public class TextCorrectionController {

    private final TextCorrectionService textCorrectionService;
    private final RequestInputValidator inputValidator;

    public TextCorrectionController(TextCorrectionService textCorrectionService, RequestInputValidator inputValidator) {
        this.textCorrectionService = textCorrectionService;
        this.inputValidator = inputValidator;
    }

    @PostMapping
    public CorrectionResponse correct(@RequestBody CorrectionRequest request) {
        inputValidator.text(request == null ? null : request.text());
        return textCorrectionService.correct(request);
    }
}
