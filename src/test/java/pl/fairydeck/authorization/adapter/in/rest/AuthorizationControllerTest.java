package pl.fairydeck.authorization.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import pl.fairydeck.authorization.application.AuthorizePurchase;
import pl.fairydeck.authorization.application.CardNotFoundException;
import pl.fairydeck.authorization.application.IdempotencyKeyReusedException;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.authorization.DeclineReason;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.money.Money;

@WebMvcTest(AuthorizationController.class)
@MockitoBean(types = AuthorizePurchase.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AuthorizationControllerTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Instant NOW = Instant.parse("2026-09-07T10:00:00Z");
    private static final UUID CARD_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final String IDEMPOTENCY_KEY = "req-1";

    private final MockMvcTester mvc;
    private final AuthorizePurchase authorizePurchase;

    AuthorizationControllerTest(MockMvcTester mvc, AuthorizePurchase authorizePurchase) {
        this.mvc = mvc;
        this.authorizePurchase = authorizePurchase;
    }

    @Test
    void createsAnApprovedAuthorizationAndPointsAtIt() {
        Authorization approved = authorization(AuthorizationStatus.APPROVED, null, NOW.plusSeconds(3600));
        given(authorizePurchase.authorize(any())).willReturn(approved);

        MvcTestResult result = post(request("12.34", "GBP"), IDEMPOTENCY_KEY);

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/v1/authorizations/" + approved.id())
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "id": "%s",
                          "cardId": "%s",
                          "amount": 12.34,
                          "currency": "GBP",
                          "merchant": "Coffee Corner",
                          "status": "APPROVED",
                          "createdAt": "2026-09-07T10:00:00Z",
                          "expiresAt": "2026-09-07T11:00:00Z"
                        }
                        """.formatted(approved.id(), CARD_ID));
    }

    @Test
    void createsADeclinedAuthorizationWithItsReason() {
        given(authorizePurchase.authorize(any()))
                .willReturn(authorization(AuthorizationStatus.DECLINED, DeclineReason.INSUFFICIENT_FUNDS, null));

        MvcTestResult result = post(request("12.34", "GBP"), IDEMPOTENCY_KEY);

        assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson()
                .isLenientlyEqualTo("""
                        { "status": "DECLINED", "declineReason": "INSUFFICIENT_FUNDS" }
                        """);
        assertThat(result).bodyJson().extractingPath("$.expiresAt").isNull();
    }

    @Test
    void handsThePurchaseAndTheIdempotencyKeyToTheUseCase() {
        given(authorizePurchase.authorize(any()))
                .willReturn(authorization(AuthorizationStatus.APPROVED, null, NOW.plusSeconds(3600)));

        post(request("12.34", "GBP"), IDEMPOTENCY_KEY);

        ArgumentCaptor<Purchase> purchase = ArgumentCaptor.forClass(Purchase.class);
        verify(authorizePurchase).authorize(purchase.capture());
        assertThat(purchase.getValue())
                .isEqualTo(new Purchase(CARD_ID, Money.of("12.34", GBP), "Coffee Corner", IDEMPOTENCY_KEY));
    }

    @Test
    void rejectsARequestWithoutAnIdempotencyKey() {
        MvcTestResult result = post(request("12.34", "GBP"), null);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void rejectsANonPositiveAmount() {
        MvcTestResult result = post(request("-1.00", "GBP"), IDEMPOTENCY_KEY);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void rejectsAnAmountFinerThanTheCurrencyAllows() {
        MvcTestResult result = post(request("12.345", "GBP"), IDEMPOTENCY_KEY);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().contains("GBP");
    }

    @Test
    void answersNotFoundForAnUnknownCard() {
        given(authorizePurchase.authorize(any())).willThrow(new CardNotFoundException(CARD_ID));

        MvcTestResult result = post(request("12.34", "GBP"), IDEMPOTENCY_KEY);

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void answersConflictWhenTheIdempotencyKeyWasUsedForAnotherPurchase() {
        given(authorizePurchase.authorize(any())).willThrow(new IdempotencyKeyReusedException(IDEMPOTENCY_KEY));

        MvcTestResult result = post(request("12.34", "GBP"), IDEMPOTENCY_KEY);

        assertThat(result).hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private MvcTestResult post(String body, String idempotencyKey) {
        var request = mvc.post().uri("/v1/authorizations").contentType(MediaType.APPLICATION_JSON).content(body);
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return request.exchange();
    }

    private static String request(String amount, String currency) {
        return """
                { "cardId": "%s", "amount": %s, "currency": "%s", "merchant": "Coffee Corner" }
                """.formatted(CARD_ID, amount, currency);
    }

    private static Authorization authorization(AuthorizationStatus status, DeclineReason reason, Instant expiresAt) {
        return new Authorization(UUID.randomUUID(), CARD_ID, Money.of("12.34", GBP), "Coffee Corner", IDEMPOTENCY_KEY,
                status, reason, NOW, expiresAt);
    }
}
