package pl.fairydeck.authorization.technical.logbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/authorizations");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void generatesACorrelationIdWhenTheCallerSendsNone() throws Exception {
        AtomicReference<String> seenInsideTheChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seenInsideTheChain.set(MDC.get(CorrelationId.MDC_KEY)));

        assertThat(seenInsideTheChain.get()).isNotBlank();
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo(seenInsideTheChain.get());
    }

    @Test
    void propagatesTheCallersCorrelationId() throws Exception {
        request.addHeader(CorrelationId.HEADER, "caller-supplied-id");
        AtomicReference<String> seenInsideTheChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seenInsideTheChain.set(MDC.get(CorrelationId.MDC_KEY)));

        assertThat(seenInsideTheChain.get()).isEqualTo("caller-supplied-id");
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("caller-supplied-id");
    }

    @Test
    void clearsTheCorrelationIdOnceTheRequestIsDone() throws Exception {
        filter.doFilter(request, response, (req, res) -> { });

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void clearsTheCorrelationIdEvenWhenTheRequestFails() {
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("boom");
        })).isInstanceOf(ServletException.class);

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void logsOneLinePerRequestWithStatusAndDuration(CapturedOutput output) throws Exception {
        response.setStatus(201);

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(output).contains("POST /v1/authorizations").contains("201").contains("ms");
    }
}
