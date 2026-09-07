package pl.fairydeck.authorization.technical.logbook;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes the correlation id for the request, echoes it back to the caller and logs one line per request
 * with the status and the time it took.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = CorrelationId.fromHeaderOrNew(request.getHeader(CorrelationId.HEADER));
        long startedAt = System.nanoTime();
        MDC.put(CorrelationId.MDC_KEY, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
            log.info("{} {} responded {} in {} ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsed.toMillis());
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
