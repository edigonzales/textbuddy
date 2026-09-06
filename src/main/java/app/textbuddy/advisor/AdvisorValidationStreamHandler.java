package app.textbuddy.advisor;

public interface AdvisorValidationStreamHandler {

    void validation(AdvisorValidationEvent event);

    void progress(AdvisorProgressEvent event);

    void complete();

    void error(String message);
}
