package pl.fairydeck.authorization.domain.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.fairydeck.authorization.domain.ledger.LedgerEntryType.CAPTURE;
import static pl.fairydeck.authorization.domain.ledger.LedgerEntryType.HOLD;
import static pl.fairydeck.authorization.domain.ledger.LedgerEntryType.HOLD_RELEASE;
import static pl.fairydeck.authorization.domain.ledger.LedgerEntryType.REFUND;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.domain.money.CurrencyMismatchException;
import pl.fairydeck.authorization.domain.money.Money;

class BalanceTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency JPY = Currency.getInstance("JPY");
    private static final UUID CARD_ID = UUID.randomUUID();
    private static final Money CREDIT_LIMIT = gbp("100.00");

    @Test
    void availableBalanceEqualsTheCreditLimitWhenTheLedgerIsEmpty() {
        Balance balance = Balance.derive(CREDIT_LIMIT, List.of());

        assertThat(balance.available()).isEqualTo(CREDIT_LIMIT);
        assertThat(balance.pending()).isEqualTo(Money.zero(GBP));
        assertThat(balance.settled()).isEqualTo(Money.zero(GBP));
    }

    @Test
    void aHoldReducesTheAvailableBalanceAndShowsAsPending() {
        Balance balance = Balance.derive(CREDIT_LIMIT, List.of(entry(HOLD, "30.00")));

        assertThat(balance.pending()).isEqualTo(gbp("30.00"));
        assertThat(balance.settled()).isEqualTo(Money.zero(GBP));
        assertThat(balance.available()).isEqualTo(gbp("70.00"));
    }

    @Test
    void releasingAHoldRestoresTheAvailableBalance() {
        Balance balance = Balance.derive(CREDIT_LIMIT, List.of(
                entry(HOLD, "30.00"),
                entry(HOLD_RELEASE, "30.00")));

        assertThat(balance.pending()).isEqualTo(Money.zero(GBP));
        assertThat(balance.available()).isEqualTo(CREDIT_LIMIT);
    }

    @Test
    void capturingMovesTheAmountFromPendingToSettledWithoutChangingAvailability() {
        Balance balance = Balance.derive(CREDIT_LIMIT, List.of(
                entry(HOLD, "30.00"),
                entry(HOLD_RELEASE, "30.00"),
                entry(CAPTURE, "30.00")));

        assertThat(balance.pending()).isEqualTo(Money.zero(GBP));
        assertThat(balance.settled()).isEqualTo(gbp("30.00"));
        assertThat(balance.available()).isEqualTo(gbp("70.00"));
    }

    @Test
    void aRefundRestoresTheAvailableBalance() {
        Balance balance = Balance.derive(CREDIT_LIMIT, List.of(
                entry(HOLD, "30.00"),
                entry(HOLD_RELEASE, "30.00"),
                entry(CAPTURE, "30.00"),
                entry(REFUND, "30.00")));

        assertThat(balance.settled()).isEqualTo(Money.zero(GBP));
        assertThat(balance.available()).isEqualTo(CREDIT_LIMIT);
    }

    @Test
    void isDerivedFromTheWholeHistoryRatherThanTheLatestEntry() {
        Balance balance = Balance.derive(CREDIT_LIMIT, List.of(
                entry(HOLD, "30.00"),
                entry(HOLD, "20.00"),
                entry(HOLD_RELEASE, "30.00"),
                entry(HOLD, "5.00")));

        assertThat(balance.pending()).isEqualTo(gbp("25.00"));
        assertThat(balance.available()).isEqualTo(gbp("75.00"));
    }

    @Test
    void refusesEntriesInAnotherCurrency() {
        LedgerEntry yenHold = new LedgerEntry(
                UUID.randomUUID(), CARD_ID, HOLD, Money.of("1000", JPY), UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> Balance.derive(CREDIT_LIMIT, List.of(yenHold)))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    private static LedgerEntry entry(LedgerEntryType type, String amount) {
        return new LedgerEntry(UUID.randomUUID(), CARD_ID, type, gbp(amount), UUID.randomUUID(), Instant.now());
    }

    private static Money gbp(String amount) {
        return Money.of(amount, GBP);
    }
}
