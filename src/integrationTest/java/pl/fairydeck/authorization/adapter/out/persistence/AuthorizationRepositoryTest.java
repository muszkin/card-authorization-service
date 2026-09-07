package pl.fairydeck.authorization.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import pl.fairydeck.authorization.IntegrationTest;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.application.port.out.DuplicateIdempotencyKeyException;
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

    private Authorization approvedWithKey(String idempotencyKey) {
        return new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP), "Coffee Corner", idempotencyKey,
                AuthorizationStatus.APPROVED, null, NOW, NOW.plusSeconds(3600));
    }
}
