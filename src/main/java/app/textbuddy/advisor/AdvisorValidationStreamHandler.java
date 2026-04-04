package app.textbuddy.advisor;

public interface AdvisorValidationStreamHandler {

    void validation(AdvisorValidationEvent event);

    void complete();

    void error(String message);
}
