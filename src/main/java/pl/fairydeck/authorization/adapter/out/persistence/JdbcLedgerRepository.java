package pl.fairydeck.authorization.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.fairydeck.authorization.application.port.out.LedgerRepository;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;

/**
 * Only ever inserts. The table carries a trigger that rejects UPDATE and DELETE, so append-only is a property
 * of the schema rather than a convention of this class. Per-card serialization uses a transaction-scoped
 * advisory lock instead of locking the card row: the card itself is served from the cache and never updated
 * here, so there is no row to lock and nothing to escalate.
 */
@Repository
class JdbcLedgerRepository implements LedgerRepository {

    private static final String COLUMNS = "id, card_id, authorization_id, type, amount_minor, currency, created_at";

    private final JdbcClient jdbc;

    JdbcLedgerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lock(UUID cardId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("A card ledger lock only makes sense inside a transaction");
        }
        jdbc.sql("SELECT pg_advisory_xact_lock(hashtext(:cardId))")
                .param("cardId", cardId.toString())
                .query()
                .singleValue();
    }

    @Override
    public void append(LedgerEntry entry) {
        jdbc.sql("INSERT INTO ledger_entries (" + COLUMNS + ") "
                        + "VALUES (:id, :cardId, :authorizationId, :type, :amountMinor, :currency, :createdAt)")
                .param("id", entry.id())
                .param("cardId", entry.cardId())
                .param("authorizationId", entry.authorizationId())
                .param("type", entry.type().name())
                .param("amountMinor", entry.amount().minorUnits())
                .param("currency", entry.amount().currency().getCurrencyCode())
                .param("createdAt", Columns.timestamp(entry.createdAt()))
                .update();
    }

    @Override
    public List<LedgerEntry> entriesFor(UUID cardId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM ledger_entries WHERE card_id = :cardId ORDER BY position")
                .param("cardId", cardId)
                .query(JdbcLedgerRepository::toEntry)
                .list();
    }

    private static LedgerEntry toEntry(ResultSet row, int rowNumber) throws SQLException {
        return new LedgerEntry(
                Columns.uuid(row, "id"),
                Columns.uuid(row, "card_id"),
                LedgerEntryType.valueOf(row.getString("type")),
                Columns.money(row, "amount_minor", "currency"),
                Columns.uuid(row, "authorization_id"),
                Columns.instant(row, "created_at"));
    }
}
