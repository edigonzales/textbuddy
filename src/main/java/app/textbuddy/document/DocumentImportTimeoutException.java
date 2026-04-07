package app.textbuddy.document;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class DocumentImportTimeoutException extends RuntimeException {

    public DocumentImportTimeoutException(String message) {
        super(message);
    }

    public DocumentImportTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
