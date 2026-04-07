package app.textbuddy.document;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
public class DocumentUploadTooLargeException extends RuntimeException {

    public DocumentUploadTooLargeException(String message) {
        super(message);
    }
}
