package pl.fairydeck.authorization.application.port.out;

import java.util.List;
import java.util.UUID;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;

public interface LedgerRepository {

    void append(LedgerEntry entry);

    List<LedgerEntry> entriesFor(UUID cardId);
}
