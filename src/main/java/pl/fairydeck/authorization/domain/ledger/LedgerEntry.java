package pl.fairydeck.authorization.domain.ledger;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import pl.fairydeck.authorization.domain.money.Money;

/**
 * An immutable fact about money movement on a card. Entries are only ever appended; a correction is a new
 * entry of the opposite type, never an edit.
 */
public record LedgerEntry(
        UUID id,
        UUID cardId,
        LedgerEntryType type,
        Money amount,
        UUID authorizationId,
        Instant createdAt) {

    public LedgerEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(cardId, "cardId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(authorizationId, "authorizationId");
        Objects.requireNonNull(createdAt, "createdAt");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "Ledger entry amount must be positive; direction comes from the entry type");
        }
    }
}
