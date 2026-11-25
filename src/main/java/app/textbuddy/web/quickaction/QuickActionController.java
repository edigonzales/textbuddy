package app.textbuddy.web.quickaction;

import app.textbuddy.quickaction.QuickActionRequest;
import app.textbuddy.quickaction.QuickActionResponse;
import app.textbuddy.quickaction.QuickActionService;
import app.textbuddy.quickaction.QuickActionType;
import app.textbuddy.web.RequestInputValidator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/quick-actions")
public final class QuickActionController {

    private final QuickActionService quickActionService;
    private final RequestInputValidator inputValidator;

    public QuickActionController(
            QuickActionService quickActionService,
            RequestInputValidator inputValidator
    ) {
        this.quickActionService = quickActionService;
        this.inputValidator = inputValidator;
    }

    @PostMapping("/{action}")
    public QuickActionResponse execute(
            @PathVariable String action,
            @RequestBody QuickActionRequest request
    ) {
        QuickActionType type = QuickActionType.fromPath(action)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quick Action ist ungültig."));

        inputValidator.text(request == null ? null : request.text());

        return quickActionService.execute(type, request);
    }
}
