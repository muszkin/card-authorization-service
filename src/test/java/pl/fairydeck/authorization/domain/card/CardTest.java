package pl.fairydeck.authorization.domain.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.domain.money.Money;

class CardTest {

    private static final Currency GBP = Currency.getInstance("GBP");

    @Test
    void issuesAnActiveCardWithItsOwnIdentity() {
        UUID cardholderId = UUID.randomUUID();

        Card card = Card.issue(cardholderId, Money.of("500.00", GBP));

        assertThat(card.id()).isNotNull();
        assertThat(card.cardholderId()).isEqualTo(cardholderId);
        assertThat(card.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.isActive()).isTrue();
        assertThat(card.creditLimit()).isEqualTo(Money.of("500.00", GBP));
    }

    @Test
    void requiresAPositiveCreditLimit() {
        assertThatIllegalArgumentException().isThrownBy(() -> Card.issue(UUID.randomUUID(), Money.zero(GBP)));
    }
}
