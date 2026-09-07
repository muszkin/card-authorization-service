package pl.fairydeck.authorization.technical.httpclient;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;

/**
 * Shape of every outbound HTTP call: one immediate retry on 429/503, never a retry after a timeout, and the
 * current correlation id forwarded. Timeouts themselves are configured under {@code spring.http.clients}.
 */
public final class OutboundHttp {

    private static final int MAX_RETRIES = 1;

    private OutboundHttp() {
    }

    public static HttpComponentsClientHttpRequestFactoryBuilder customize(
            HttpComponentsClientHttpRequestFactoryBuilder builder) {
        return builder.withHttpClientCustomizer(httpClient -> httpClient
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(MAX_RETRIES, TimeValue.ZERO_MILLISECONDS))
                .addRequestInterceptorFirst(new CorrelationIdHeader()));
    }
}
