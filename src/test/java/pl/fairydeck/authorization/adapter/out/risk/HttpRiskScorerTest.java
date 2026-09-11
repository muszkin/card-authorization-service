package pl.fairydeck.authorization.adapter.out.risk;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.time.Duration;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.web.client.RestClient;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;
import pl.fairydeck.authorization.domain.money.Money;
import pl.fairydeck.authorization.technical.httpclient.OutboundHttp;

@WireMockTest
class HttpRiskScorerTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);
    private static final String SCORES = "/v1/scores";

    private final Purchase purchase = new Purchase(
            UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"),
            Money.of("12.34", Currency.getInstance("GBP")),
            "Coffee Corner",
            "idempotency-key");

    private HttpRiskScorer scorer;

    @BeforeEach
    void pointTheScorerAtWireMock(WireMockRuntimeInfo wireMock) {
        var requestFactory = OutboundHttp.customize(ClientHttpRequestFactoryBuilder.httpComponents())
                .build(HttpClientSettings.defaults().withTimeouts(TIMEOUT, TIMEOUT));
        RestClient client = RestClient.builder().requestFactory(requestFactory).baseUrl(wireMock.getHttpBaseUrl()).build();
        scorer = new HttpRiskScorer(client);
    }

    @Test
    void returnsTheScoreComputedByTheRiskService() {
        stubFor(post(SCORES).willReturn(okJson("{\"score\": 42}")));

        assertThat(scorer.assess(purchase)).isEqualTo(new RiskAssessment.Scored(42));
    }

    @Test
    void preservesAnIntegralRiskScore() {
        stubFor(post(SCORES).willReturn(okJson("{\"score\": 70}")));

        assertThat(scorer.assess(purchase)).isEqualTo(new RiskAssessment.Scored(70));
    }

    @Test
    void preservesAnIntegralDecimalRiskScore() {
        stubFor(post(SCORES).willReturn(okJson("{\"score\": 70.0}")));

        assertThat(scorer.assess(purchase)).isEqualTo(new RiskAssessment.Scored(70));
    }

    @Test
    void reportsUnavailabilityWhenTheRiskServiceReturnsANonIntegralScore() {
        stubFor(post(SCORES).willReturn(okJson("{\"score\": 70.9}")));

        assertThat(scorer.assess(purchase)).isInstanceOf(RiskAssessment.Unavailable.class);
    }

    @Test
    void reportsUnavailabilityWhenTheRiskServiceOmitsTheScore() {
        stubFor(post(SCORES).willReturn(okJson("{}")));

        assertThat(scorer.assess(purchase)).isInstanceOf(RiskAssessment.Unavailable.class);
    }

    @Test
    void reportsUnavailabilityWhenTheRiskServiceReturnsANullScore() {
        stubFor(post(SCORES).willReturn(okJson("{\"score\": null}")));

        assertThat(scorer.assess(purchase)).isInstanceOf(RiskAssessment.Unavailable.class);
    }

    @Test
    void reportsUnavailabilityWhenTheRiskServiceReturnsMalformedJson() {
        stubFor(post(SCORES).willReturn(okJson("{\"score\":")));

        assertThat(scorer.assess(purchase)).isInstanceOf(RiskAssessment.Unavailable.class);
    }

    @Test
    void reportsUnavailabilityWhenTheRiskServiceReturnsAnOutOfRangeScore() {
        stubFor(post(SCORES).willReturn(okJson("{\"score\": 2147483648}")));

        assertThat(scorer.assess(purchase)).isInstanceOf(RiskAssessment.Unavailable.class);
    }

    @Test
    void describesThePurchaseToTheRiskService() {
        stubFor(post(SCORES).willReturn(okJson("{\"score\": 1}")));

        scorer.assess(purchase);

        verify(postRequestedFor(urlEqualTo(SCORES))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson("""
                        {
                          "cardId": "0f8fad5b-d9cb-469f-a165-70867728950e",
                          "amount": 12.34,
                          "currency": "GBP",
                          "merchant": "Coffee Corner"
                        }
                        """)));
    }

    @Test
    void reportsUnavailabilityWithinTheTimeoutWhenTheRiskServiceIsTooSlow() {
        int wellBeyondTheTimeout = (int) TIMEOUT.multipliedBy(3).toMillis();
        stubFor(post(SCORES).willReturn(okJson("{\"score\": 1}").withFixedDelay(wellBeyondTheTimeout)));
        long startedAt = System.nanoTime();

        RiskAssessment assessment = scorer.assess(purchase);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        assertThat(assessment).isInstanceOf(RiskAssessment.Unavailable.class);
        assertThat(elapsed).isLessThan(TIMEOUT.multipliedBy(2));
    }

    @Test
    void reportsUnavailabilityWhenTheRiskServiceKeepsFailing() {
        stubFor(post(SCORES).willReturn(serviceUnavailable()));

        assertThat(scorer.assess(purchase)).isInstanceOf(RiskAssessment.Unavailable.class);
        verify(2, postRequestedFor(urlEqualTo(SCORES)));
    }

    @Test
    void reportsUnavailabilityWhenTheRiskServiceErrors() {
        stubFor(post(SCORES).willReturn(serverError()));

        assertThat(scorer.assess(purchase)).isInstanceOf(RiskAssessment.Unavailable.class);
    }
}
