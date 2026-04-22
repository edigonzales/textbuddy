package app.textbuddy.integration.support;

public final class RetriableAdapterException extends RuntimeException {

    public RetriableAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
