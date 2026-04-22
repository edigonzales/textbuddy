package app.textbuddy.document;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class DocumentImportServiceUnavailableException extends RuntimeException {

    public DocumentImportServiceUnavailableException(String message) {
        super(message);
    }

    public DocumentImportServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
