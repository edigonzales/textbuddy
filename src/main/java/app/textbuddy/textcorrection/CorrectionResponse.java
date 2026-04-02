package app.textbuddy.textcorrection;

import java.util.List;

public record CorrectionResponse(String original, List<CorrectionBlock> blocks) {
}
