package pl.fairydeck.authorization.domain.authorization;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.domain.money.Money;

class PurchaseTest {

    private static final Currency GBP = Currency.getInstance("GBP");

    @Test
    void requiresAPositiveAmount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Purchase(UUID.randomUUID(), Money.zero(GBP), "Coffee Corner", "key"));
    }

    @Test
    void requiresAMerchantAndAnIdempotencyKey() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Purchase(UUID.randomUUID(), Money.of("1.00", GBP), " ", "key"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Purchase(UUID.randomUUID(), Money.of("1.00", GBP), "Coffee Corner", ""));
    }
}
