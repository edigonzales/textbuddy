package app.textbuddy.textcorrection;

public interface TextCorrectionService {

    CorrectionResponse correct(CorrectionRequest request);
}
