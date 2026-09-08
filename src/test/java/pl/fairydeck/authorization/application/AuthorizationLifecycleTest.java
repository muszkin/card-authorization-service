package pl.fairydeck.authorization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.application.port.out.InMemoryAuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.InMemoryLedgerRepository;
import pl.fairydeck.authorization.application.port.out.InMemoryOutbox;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationEvent;
import pl.fairydeck.authorization.domain.authorization.AuthorizationPolicy;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.ledger.Balance;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;
import pl.fairydeck.authorization.domain.money.Money;

class AuthorizationLifecycleTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Instant AUTHORIZED_AT = Instant.parse("2026-09-07T10:00:00Z");
    private static final Duration HOLD_VALIDITY = Duration.ofDays(7);

    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final InMemoryAuthorizationRepository authorizations = new InMemoryAuthorizationRepository();
    private final InMemoryOutbox outbox = new InMemoryOutbox();
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, gbp("100.00"));

    private Authorization approved;

    @BeforeEach
    void holdThirtyPoundsOnTheCard() {
        Purchase purchase = new Purchase(card.id(), gbp("30.00"), "Coffee Corner", "idempotency-key");
        approved = new AuthorizationPolicy(70, HOLD_VALIDITY).authorize(purchase, card,
                Balance.derive(card.creditLimit(), List.of()), new RiskAssessment.Scored(0), AUTHORIZED_AT);
        authorizations.save(approved);
        ledger.append(approved.hold());
    }

    @Test
    void captureSettlesTheHoldAndAnnouncesIt() {
        Instant captureTime = AUTHORIZED_AT.plusSeconds(3600);

        Authorization captured = lifecycleAt(captureTime).capture(approved.id());

        assertThat(captured.status()).isEqualTo(AuthorizationStatus.CAPTURED);
        assertThat(authorizations.findById(approved.id())).get().extracting(Authorization::status)
                .isEqualTo(AuthorizationStatus.CAPTURED);
        assertThat(ledger.entriesFor(card.id())).extracting(LedgerEntry::type)
                .containsExactly(LedgerEntryType.HOLD, LedgerEntryType.HOLD_RELEASE, LedgerEntryType.CAPTURE);
        assertThat(balance().settled()).isEqualTo(gbp("30.00"));
        assertThat(balance().available()).isEqualTo(gbp("70.00"));
        assertThat(outbox.recorded()).extracting(AuthorizationEvent::status, AuthorizationEvent::occurredAt)
                .containsExactly(tuple(AuthorizationStatus.CAPTURED, captureTime));
    }

    @Test
    void reversalRestoresTheAvailableBalance() {
        Authorization reversed = lifecycleAt(AUTHORIZED_AT.plusSeconds(60)).reverse(approved.id());

        assertThat(reversed.status()).isEqualTo(AuthorizationStatus.REVERSED);
        assertThat(balance().available()).isEqualTo(card.creditLimit());
        assertThat(outbox.recorded()).extracting(AuthorizationEvent::status).containsExactly(AuthorizationStatus.REVERSED);
    }

    @Test
    void aRetriedCaptureIsAnsweredFromTheCurrentStateWithoutBookingAgain() {
        AuthorizationLifecycle lifecycle = lifecycleAt(AUTHORIZED_AT.plusSeconds(3600));
        lifecycle.capture(approved.id());

        Authorization replayed = lifecycle.capture(approved.id());

        assertThat(replayed.status()).isEqualTo(AuthorizationStatus.CAPTURED);
        assertThat(ledger.entriesFor(card.id())).extracting(LedgerEntry::type)
                .containsExactly(LedgerEntryType.HOLD, LedgerEntryType.HOLD_RELEASE, LedgerEntryType.CAPTURE);
        assertThat(outbox.recorded()).hasSize(1);
    }

    @Test
    void aRetriedReverseIsAnsweredFromTheCurrentStateWithoutBookingAgain() {
        AuthorizationLifecycle lifecycle = lifecycleAt(AUTHORIZED_AT.plusSeconds(60));
        lifecycle.reverse(approved.id());

        Authorization replayed = lifecycle.reverse(approved.id());

        assertThat(replayed.status()).isEqualTo(AuthorizationStatus.REVERSED);
        assertThat(ledger.entriesFor(card.id())).extracting(LedgerEntry::type)
                .containsExactly(LedgerEntryType.HOLD, LedgerEntryType.HOLD_RELEASE);
        assertThat(outbox.recorded()).hasSize(1);
    }

    @Test
    void reverseOnAnExpiredAuthorizationReturnsItWithoutBookingAgain() {
        Instant afterExpiry = AUTHORIZED_AT.plus(HOLD_VALIDITY);
        lifecycleAt(afterExpiry).releaseExpiredHolds();

        Authorization replayed = lifecycleAt(afterExpiry.plusSeconds(60)).reverse(approved.id());

        assertThat(replayed.status()).isEqualTo(AuthorizationStatus.EXPIRED);
        assertThat(ledger.entriesFor(card.id())).extracting(LedgerEntry::type)
                .containsExactly(LedgerEntryType.HOLD, LedgerEntryType.HOLD_RELEASE);
        assertThat(outbox.recorded()).hasSize(1);
    }

    @Test
    void captureAfterReversalStillThrows() {
        AuthorizationLifecycle lifecycle = lifecycleAt(AUTHORIZED_AT.plusSeconds(60));
        lifecycle.reverse(approved.id());

        assertThatThrownBy(() -> lifecycle.capture(approved.id())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reportsAnUnknownAuthorization() {
        UUID unknown = UUID.randomUUID();
        AuthorizationLifecycle lifecycle = lifecycleAt(AUTHORIZED_AT);

        assertThatThrownBy(() -> lifecycle.capture(unknown))
                .isInstanceOf(AuthorizationNotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }

    @Test
    void theSweepReleasesHoldsWhoseValidityHasPassedAndLeavesTheOthersAlone() {
        Purchase later = new Purchase(card.id(), gbp("10.00"), "Bakery", "another-key");
        Authorization stillValid = new AuthorizationPolicy(70, HOLD_VALIDITY).authorize(later, card, balance(),
                new RiskAssessment.Scored(0), AUTHORIZED_AT.plus(Duration.ofDays(1)));
        authorizations.save(stillValid);
        ledger.append(stillValid.hold());

        lifecycleAt(AUTHORIZED_AT.plus(HOLD_VALIDITY)).releaseExpiredHolds();

        assertThat(authorizations.findById(approved.id())).get().extracting(Authorization::status)
                .isEqualTo(AuthorizationStatus.EXPIRED);
        assertThat(authorizations.findById(stillValid.id())).get().extracting(Authorization::status)
                .isEqualTo(AuthorizationStatus.APPROVED);
        assertThat(balance().pending()).isEqualTo(gbp("10.00"));
        assertThat(outbox.recorded()).extracting(AuthorizationEvent::status).containsExactly(AuthorizationStatus.EXPIRED);
    }

    private AuthorizationLifecycle lifecycleAt(Instant now) {
        return new AuthorizationLifecycle(authorizations, ledger, outbox, Clock.fixed(now, ZoneOffset.UTC));
    }

    private Balance balance() {
        return Balance.derive(card.creditLimit(), ledger.entriesFor(card.id()));
    }

    private static Money gbp(String amount) {
        return Money.of(amount, GBP);
    }
}
