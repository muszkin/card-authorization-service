package pl.fairydeck.authorization.domain.ledger;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.domain.money.Money;

class LedgerEntryTest {

    private static final Currency GBP = Currency.getInstance("GBP");

    @Test
    void requiresAPositiveAmountBecauseDirectionComesFromTheEntryType() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LedgerEntry(
                UUID.randomUUID(), UUID.randomUUID(), LedgerEntryType.HOLD,
                Money.zero(GBP), UUID.randomUUID(), Instant.now()));

        assertThatIllegalArgumentException().isThrownBy(() -> new LedgerEntry(
                UUID.randomUUID(), UUID.randomUUID(), LedgerEntryType.HOLD_RELEASE,
                Money.of("-1.00", GBP), UUID.randomUUID(), Instant.now()));
    }
}
