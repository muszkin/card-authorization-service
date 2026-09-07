package pl.fairydeck.authorization.technical.httpclient;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import pl.fairydeck.authorization.technical.logbook.CorrelationId;

final class CorrelationIdHeader implements HttpRequestInterceptor {

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context) {
        CorrelationId.current().ifPresent(id -> request.setHeader(CorrelationId.HEADER, id));
    }
}
