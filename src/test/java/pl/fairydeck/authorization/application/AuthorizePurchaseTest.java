package pl.fairydeck.authorization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.application.port.out.InMemoryAuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.InMemoryCardCache;
import pl.fairydeck.authorization.application.port.out.InMemoryCardRepository;
import pl.fairydeck.authorization.application.port.out.InMemoryLedgerRepository;
import pl.fairydeck.authorization.application.port.out.StubRiskScorer;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationPolicy;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.authorization.DeclineReason;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.ledger.Balance;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;
import pl.fairydeck.authorization.domain.money.Money;

class AuthorizePurchaseTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Instant NOW = Instant.parse("2026-09-07T10:00:00Z");

    private final InMemoryCardRepository cards = new InMemoryCardRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final InMemoryAuthorizationRepository authorizations = new InMemoryAuthorizationRepository();
    private final StubRiskScorer riskScorer = new StubRiskScorer();
    private final AuthorizationPolicy policy = new AuthorizationPolicy(70, Duration.ofDays(7));
    private final AuthorizationBooking booking =
            new AuthorizationBooking(ledger, authorizations, policy, Clock.fixed(NOW, ZoneOffset.UTC));
    private final AuthorizePurchase authorizePurchase =
            new AuthorizePurchase(new CardLookup(new InMemoryCardCache(), cards), riskScorer, booking, authorizations);

    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, gbp("100.00"));

    @Test
    void approvesAPurchaseAndPlacesTheHoldOnTheLedger() {
        cards.save(card);

        Authorization authorization = authorizePurchase.authorize(purchase("30.00"));

        assertThat(authorization.status()).isEqualTo(AuthorizationStatus.APPROVED);
        assertThat(authorization.createdAt()).isEqualTo(NOW);
        assertThat(authorizations.findById(authorization.id())).isPresent();
        assertThat(ledger.entriesFor(card.id()))
                .singleElement()
                .satisfies(hold -> {
                    assertThat(hold.type()).isEqualTo(LedgerEntryType.HOLD);
                    assertThat(hold.amount()).isEqualTo(gbp("30.00"));
                    assertThat(hold.authorizationId()).isEqualTo(authorization.id());
                });
    }

    @Test
    void declinesAgainstTheLedgerDerivedBalanceWithoutTouchingTheLedger() {
        cards.save(card);
        ledger.append(new LedgerEntry(UUID.randomUUID(), card.id(), LedgerEntryType.HOLD, gbp("80.00"),
                UUID.randomUUID(), NOW));

        Authorization authorization = authorizePurchase.authorize(purchase("30.00"));

        assertThat(authorization.status()).isEqualTo(AuthorizationStatus.DECLINED);
        assertThat(authorization.declineReason()).contains(DeclineReason.INSUFFICIENT_FUNDS);
        assertThat(authorizations.findById(authorization.id())).isPresent();
        assertThat(ledger.entriesFor(card.id())).hasSize(1);
    }

    @Test
    void declinesWhenTheRiskEngineCannotScoreThePurchase() {
        cards.save(card);
        riskScorer.willAnswer(new RiskAssessment.Unavailable());

        Authorization authorization = authorizePurchase.authorize(purchase("30.00"));

        assertThat(authorization.declineReason()).contains(DeclineReason.RISK_UNAVAILABLE);
        assertThat(riskScorer.asked()).containsExactly(purchase("30.00"));
    }

    @Test
    void refusesAnUnknownCardBeforeSpendingARiskCall() {
        Purchase onUnknownCard = purchase("30.00");

        assertThatThrownBy(() -> authorizePurchase.authorize(onUnknownCard))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining(card.id().toString());
        assertThat(riskScorer.asked()).isEmpty();
    }

    @Test
    void replaysTheStoredDecisionWhenTheSameRequestIsRetried() {
        cards.save(card);
        Authorization first = authorizePurchase.authorize(purchase("30.00"));

        Authorization retry = authorizePurchase.authorize(purchase("30.00"));

        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(ledger.entriesFor(card.id())).hasSize(1);
        assertThat(riskScorer.asked()).hasSize(1);
    }

    @Test
    void refusesToReuseAnIdempotencyKeyForADifferentPurchase() {
        cards.save(card);
        authorizePurchase.authorize(purchase("30.00"));

        assertThatThrownBy(() -> authorizePurchase.authorize(purchase("40.00")))
                .isInstanceOf(IdempotencyKeyReusedException.class)
                .hasMessageContaining("idempotency-key");
        assertThat(ledger.entriesFor(card.id())).hasSize(1);
    }

    @Test
    void recoversWhenAnIdenticalRetryIsBookedWhileTheRiskEngineIsStillThinking() {
        cards.save(card);
        Authorization bookedByTheRetry = policy.authorize(purchase("30.00"), card,
                Balance.derive(card.creditLimit(), List.of()), new RiskAssessment.Scored(0), NOW);
        riskScorer.whileAssessing(() -> {
            authorizations.save(bookedByTheRetry);
            ledger.append(bookedByTheRetry.hold());
        });

        Authorization result = authorizePurchase.authorize(purchase("30.00"));

        assertThat(result.id()).isEqualTo(bookedByTheRetry.id());
        assertThat(ledger.entriesFor(card.id())).hasSize(1);
    }

    private Purchase purchase(String amount) {
        return new Purchase(card.id(), gbp(amount), "Coffee Corner", "idempotency-key");
    }

    private static Money gbp(String amount) {
        return Money.of(amount, GBP);
    }
}
