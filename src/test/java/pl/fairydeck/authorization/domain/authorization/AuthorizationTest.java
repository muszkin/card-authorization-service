package pl.fairydeck.authorization.domain.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.ledger.Balance;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;
import pl.fairydeck.authorization.domain.money.Money;

class AuthorizationTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Instant NOW = Instant.parse("2026-09-07T10:00:00Z");

    private final AuthorizationPolicy policy = new AuthorizationPolicy(70, Duration.ofDays(7));
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, Money.of("100.00", GBP));
    private final Balance untouchedLimit = Balance.derive(card.creditLimit(), List.of());
    private final Purchase purchase = new Purchase(card.id(), Money.of("30.00", GBP), "Coffee Corner", "idempotency-key");

    @Test
    void anApprovedAuthorizationPlacesAHoldForItsAmount() {
        Authorization approved = policy.authorize(purchase, card, untouchedLimit, new RiskAssessment.Scored(10), NOW);

        LedgerEntry hold = approved.hold();

        assertThat(hold.type()).isEqualTo(LedgerEntryType.HOLD);
        assertThat(hold.amount()).isEqualTo(purchase.amount());
        assertThat(hold.cardId()).isEqualTo(card.id());
        assertThat(hold.authorizationId()).isEqualTo(approved.id());
        assertThat(hold.createdAt()).isEqualTo(NOW);
    }

    @Test
    void aDeclinedAuthorizationCannotPlaceAHold() {
        Authorization declined = policy.authorize(purchase, card, untouchedLimit, new RiskAssessment.Unavailable(), NOW);

        assertThatIllegalStateException().isThrownBy(declined::hold);
    }
}
