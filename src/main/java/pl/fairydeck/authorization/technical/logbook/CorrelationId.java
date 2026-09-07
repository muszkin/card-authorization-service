package pl.fairydeck.authorization.technical.logbook;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * The identifier that ties together every log line and outbound call made while serving one request.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
    }

    public static Optional<String> current() {
        return Optional.ofNullable(MDC.get(MDC_KEY));
    }

    static String fromHeaderOrNew(String headerValue) {
        return headerValue == null || headerValue.isBlank() ? UUID.randomUUID().toString() : headerValue;
    }
}
