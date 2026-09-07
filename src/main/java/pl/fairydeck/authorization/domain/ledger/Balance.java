package pl.fairydeck.authorization.domain.ledger;

import java.util.Collection;
import pl.fairydeck.authorization.domain.money.Money;

/**
 * Card balance derived from the ledger. Nothing stores it; it is recomputed from the entries every time.
 */
public record Balance(Money creditLimit, Money pending, Money settled) {

    public static Balance derive(Money creditLimit, Collection<LedgerEntry> entries) {
        Money pending = Money.zero(creditLimit.currency());
        Money settled = Money.zero(creditLimit.currency());
        for (LedgerEntry entry : entries) {
            switch (entry.type()) {
                case HOLD -> pending = pending.plus(entry.amount());
                case HOLD_RELEASE -> pending = pending.minus(entry.amount());
                case CAPTURE -> settled = settled.plus(entry.amount());
                case REFUND -> settled = settled.minus(entry.amount());
            }
        }
        return new Balance(creditLimit, pending, settled);
    }

    public Money available() {
        return creditLimit.minus(pending).minus(settled);
    }
}
