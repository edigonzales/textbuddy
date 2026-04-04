package app.textbuddy.document;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class DocumentConversionFailedException extends RuntimeException {

    public DocumentConversionFailedException(String message) {
        super(message);
    }

    public DocumentConversionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
