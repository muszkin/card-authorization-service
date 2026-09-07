package pl.fairydeck.authorization.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import pl.fairydeck.authorization.IntegrationTest;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.application.port.out.DuplicateIdempotencyKeyException;
import pl.fairydeck.authorization.application.port.out.TransactionFilter;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.authorization.DeclineReason;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.money.Money;

@IntegrationTest
@Transactional
class AuthorizationRepositoryTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Instant NOW = Instant.parse("2026-09-07T10:00:00Z");

    private final AuthorizationRepository authorizations;
    private final CardRepository cards;
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, Money.of("100.00", GBP));

    AuthorizationRepositoryTest(AuthorizationRepository authorizations, CardRepository cards) {
        this.authorizations = authorizations;
        this.cards = cards;
    }

    @BeforeEach
    void storeTheCard() {
        cards.save(card);
    }

    @Test
    void storesAnApprovedAuthorizationWithItsExpiry() {
        Authorization approved = new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP), "Coffee Corner",
                "key-approved", AuthorizationStatus.APPROVED, null, NOW, NOW.plusSeconds(3600));

        authorizations.save(approved);

        assertThat(authorizations.findById(approved.id())).get().usingRecursiveComparison().isEqualTo(approved);
    }

    @Test
    void storesADeclinedAuthorizationWithItsReason() {
        Authorization declined = new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP), "Coffee Corner",
                "key-declined", AuthorizationStatus.DECLINED, DeclineReason.INSUFFICIENT_FUNDS, NOW, null);

        authorizations.save(declined);

        assertThat(authorizations.findById(declined.id())).get().usingRecursiveComparison().isEqualTo(declined);
    }

    @Test
    void findsAnAuthorizationByItsIdempotencyKey() {
        Authorization approved = new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP), "Coffee Corner",
                "key-lookup", AuthorizationStatus.APPROVED, null, NOW, NOW.plusSeconds(3600));
        authorizations.save(approved);

        assertThat(authorizations.findByIdempotencyKey("key-lookup")).get().usingRecursiveComparison().isEqualTo(approved);
        assertThat(authorizations.findByIdempotencyKey("unknown-key")).isEmpty();
    }

    @Test
    void refusesASecondAuthorizationWithTheSameIdempotencyKey() {
        authorizations.save(approvedWithKey("key-used-twice"));

        assertThatThrownBy(() -> authorizations.save(approvedWithKey("key-used-twice")))
                .isInstanceOf(DuplicateIdempotencyKeyException.class)
                .hasMessageContaining("key-used-twice");
    }

    @Test
    void findsApprovedAuthorizationsWhoseHoldValidityHasPassed() {
        Authorization expired = new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP), "Coffee Corner",
                "key-expired", AuthorizationStatus.APPROVED, null, NOW.minusSeconds(7200), NOW.minusSeconds(3600));
        Authorization stillValid = approvedWithKey("key-valid");
        Authorization capturedLongAgo = new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP),
                "Coffee Corner", "key-captured", AuthorizationStatus.CAPTURED, null, NOW.minusSeconds(7200),
                NOW.minusSeconds(3600));
        authorizations.save(expired);
        authorizations.save(stillValid);
        authorizations.save(capturedLongAgo);

        assertThat(authorizations.findExpiredHolds(NOW)).extracting(Authorization::id).containsExactly(expired.id());
    }

    @Test
    void filtersACardsTransactionsByTimeWindowAndStatusNewestFirst() {
        Instant threeDaysAgo = NOW.minus(Duration.ofDays(3));
        Instant twoDaysAgo = NOW.minus(Duration.ofDays(2));
        Instant yesterday = NOW.minus(Duration.ofDays(1));
        Authorization oldApproved = createdAt(threeDaysAgo, AuthorizationStatus.APPROVED, "key-old");
        Authorization declined = createdAt(twoDaysAgo, AuthorizationStatus.DECLINED, "key-declined");
        Authorization recentApproved = createdAt(yesterday, AuthorizationStatus.APPROVED, "key-recent");
        Card otherCard = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, Money.of("100.00", GBP));
        cards.save(otherCard);
        Authorization onAnotherCard = new Authorization(UUID.randomUUID(), otherCard.id(), Money.of("1.00", GBP), "Kiosk",
                "key-other-card", AuthorizationStatus.APPROVED, null, yesterday, NOW);
        for (Authorization authorization : List.of(oldApproved, declined, recentApproved, onAnotherCard)) {
            authorizations.save(authorization);
        }

        assertThat(authorizations.findByCard(card.id(), TransactionFilter.none()))
                .extracting(Authorization::id).containsExactly(recentApproved.id(), declined.id(), oldApproved.id());
        assertThat(authorizations.findByCard(card.id(), new TransactionFilter(null, null, AuthorizationStatus.APPROVED)))
                .extracting(Authorization::id).containsExactly(recentApproved.id(), oldApproved.id());
        assertThat(authorizations.findByCard(card.id(), new TransactionFilter(twoDaysAgo, yesterday, null)))
                .extracting(Authorization::id).containsExactly(declined.id());
        assertThat(authorizations.findByCard(card.id(), new TransactionFilter(twoDaysAgo, null, AuthorizationStatus.APPROVED)))
                .extracting(Authorization::id).containsExactly(recentApproved.id());
    }

    private Authorization createdAt(Instant createdAt, AuthorizationStatus status, String idempotencyKey) {
        return new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP), "Coffee Corner", idempotencyKey,
                status, status == AuthorizationStatus.DECLINED ? DeclineReason.INSUFFICIENT_FUNDS : null, createdAt,
                status == AuthorizationStatus.APPROVED ? createdAt.plusSeconds(3600) : null);
    }

    private Authorization approvedWithKey(String idempotencyKey) {
        return new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP), "Coffee Corner", idempotencyKey,
                AuthorizationStatus.APPROVED, null, NOW, NOW.plusSeconds(3600));
    }
}
