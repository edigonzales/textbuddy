package app.textbuddy.advisor;

public interface AdvisorValidationService {

    void validate(AdvisorValidateRequest request, AdvisorValidationStreamHandler handler);
}
