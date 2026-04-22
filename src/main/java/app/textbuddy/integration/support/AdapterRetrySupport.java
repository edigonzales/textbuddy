package app.textbuddy.integration.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

public final class AdapterRetrySupport {

    private static final Logger log = LoggerFactory.getLogger(AdapterRetrySupport.class);

    private AdapterRetrySupport() {
    }

    public static <T> T withRetry(
            String adapterName,
            int maxRetries,
            Supplier<T> operation,
            java.util.function.Function<RetriableAdapterException, RuntimeException> finalFailureMapper
    ) {
        Objects.requireNonNull(operation);
        Objects.requireNonNull(finalFailureMapper);

        int attempts = Math.max(0, maxRetries) + 1;
        RetriableAdapterException lastException = null;

        for (int attempt = 1; attempt <= attempts; attempt += 1) {
            try {
                return operation.get();
            } catch (RetriableAdapterException exception) {
                lastException = exception;

                if (attempt >= attempts) {
                    break;
                }

                log.warn(
                        "{}-Aufruf fehlgeschlagen, neuer Versuch {}/{}.",
                        adapterName,
                        attempt,
                        attempts,
                        exception
                );
            }
        }

        if (lastException != null) {
            throw finalFailureMapper.apply(lastException);
        }

        throw new IllegalStateException(adapterName + "-Aufruf konnte nicht ausgeführt werden.");
    }

    public static boolean isRetriableStatusCode(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }
}
