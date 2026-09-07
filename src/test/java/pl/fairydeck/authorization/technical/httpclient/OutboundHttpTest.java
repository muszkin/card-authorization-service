package pl.fairydeck.authorization.technical.httpclient;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import pl.fairydeck.authorization.technical.logbook.CorrelationId;

@WireMockTest
class OutboundHttpTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);

    private RestClient client;

    @BeforeEach
    void buildClientAgainstWireMock(WireMockRuntimeInfo wireMock) {
        var requestFactory = OutboundHttp.customize(ClientHttpRequestFactoryBuilder.httpComponents())
                .build(HttpClientSettings.defaults().withTimeouts(TIMEOUT, TIMEOUT));
        client = RestClient.builder().requestFactory(requestFactory).baseUrl(wireMock.getHttpBaseUrl()).build();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesTheCurrentCorrelationIdDownstream() {
        stubFor(get("/ping").willReturn(ok()));
        MDC.put(CorrelationId.MDC_KEY, "corr-42");

        client.get().uri("/ping").retrieve().toBodilessEntity();

        verify(getRequestedFor(urlEqualTo("/ping")).withHeader(CorrelationId.HEADER, equalTo("corr-42")));
    }

    @Test
    void retriesOnceImmediatelyWhenTheDownstreamIsUnavailable() {
        stubFor(post("/score").inScenario("flaky").whenScenarioStateIs(STARTED)
                .willReturn(serviceUnavailable()).willSetStateTo("recovered"));
        stubFor(post("/score").inScenario("flaky").whenScenarioStateIs("recovered")
                .willReturn(ok("42")));

        String body = client.post().uri("/score").retrieve().body(String.class);

        assertThat(body).isEqualTo("42");
        verify(2, postRequestedFor(urlEqualTo("/score")));
    }

    @Test
    void givesUpAfterTheSingleRetry() {
        stubFor(post("/score").willReturn(serviceUnavailable()));

        assertThatThrownBy(() -> client.post().uri("/score").retrieve().toBodilessEntity())
                .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);
        verify(2, postRequestedFor(urlEqualTo("/score")));
    }

    @Test
    void failsFastOnTimeoutInsteadOfRetrying() {
        int wellBeyondTheTimeout = (int) TIMEOUT.multipliedBy(3).toMillis();
        stubFor(post("/score").willReturn(ok().withFixedDelay(wellBeyondTheTimeout)));

        assertThatThrownBy(() -> client.post().uri("/score").retrieve().toBodilessEntity())
                .isInstanceOf(ResourceAccessException.class)
                .hasCauseInstanceOf(SocketTimeoutException.class);
        verify(1, postRequestedFor(urlEqualTo("/score")));
    }
}
