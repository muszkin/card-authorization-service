package pl.fairydeck.authorization.application.port.out;

import java.util.List;
import java.util.UUID;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;

public interface LedgerRepository {

    /**
     * Serializes ledger appends for one card until the current transaction ends. Every balance-changing
     * transaction takes this lock before reading the ledger, so no two of them can decide on the same stale
     * balance.
     */
    void lock(UUID cardId);

    void append(LedgerEntry entry);

    List<LedgerEntry> entriesFor(UUID cardId);
}
