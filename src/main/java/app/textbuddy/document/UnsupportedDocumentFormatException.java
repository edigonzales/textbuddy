package app.textbuddy.document;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedDocumentFormatException extends RuntimeException {

    public UnsupportedDocumentFormatException(String message) {
        super(message);
    }
}
