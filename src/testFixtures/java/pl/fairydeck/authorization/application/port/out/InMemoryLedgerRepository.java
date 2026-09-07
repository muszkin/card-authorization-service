package pl.fairydeck.authorization.application.port.out;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;

public final class InMemoryLedgerRepository implements LedgerRepository {

    private final List<LedgerEntry> entries = new ArrayList<>();

    @Override
    public void append(LedgerEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<LedgerEntry> entriesFor(UUID cardId) {
        return entries.stream().filter(entry -> entry.cardId().equals(cardId)).toList();
    }
}
