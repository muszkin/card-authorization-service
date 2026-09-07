package pl.fairydeck.authorization.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.authorization.DeclineReason;

@Repository
class JdbcAuthorizationRepository implements AuthorizationRepository {

    private static final String COLUMNS = "id, card_id, amount_minor, currency, merchant, status, decline_reason, "
            + "idempotency_key, created_at, expires_at";

    private final JdbcClient jdbc;

    JdbcAuthorizationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Authorization authorization) {
        jdbc.sql("INSERT INTO authorizations (" + COLUMNS + ") VALUES (:id, :cardId, :amountMinor, :currency, "
                        + ":merchant, :status, :declineReason, :idempotencyKey, :createdAt, :expiresAt) "
                        + "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status")
                .param("id", authorization.id())
                .param("cardId", authorization.cardId())
                .param("amountMinor", authorization.amount().minorUnits())
                .param("currency", authorization.amount().currency().getCurrencyCode())
                .param("merchant", authorization.merchant())
                .param("status", authorization.status().name())
                .param("declineReason", authorization.declineReason().map(Enum::name).orElse(null), Types.VARCHAR)
                .param("idempotencyKey", authorization.idempotencyKey())
                .param("createdAt", Columns.timestamp(authorization.createdAt()))
                .param("expiresAt", Columns.timestamp(authorization.expiresAt().orElse(null)),
                        Types.TIMESTAMP_WITH_TIMEZONE)
                .update();
    }

    @Override
    public Optional<Authorization> findById(UUID id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM authorizations WHERE id = :id")
                .param("id", id)
                .query(JdbcAuthorizationRepository::toAuthorization)
                .optional();
    }

    @Override
    public Optional<Authorization> findByIdempotencyKey(String idempotencyKey) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM authorizations WHERE idempotency_key = :idempotencyKey")
                .param("idempotencyKey", idempotencyKey)
                .query(JdbcAuthorizationRepository::toAuthorization)
                .optional();
    }

    private static Authorization toAuthorization(ResultSet row, int rowNumber) throws SQLException {
        String declineReason = row.getString("decline_reason");
        return new Authorization(
                Columns.uuid(row, "id"),
                Columns.uuid(row, "card_id"),
                Columns.money(row, "amount_minor", "currency"),
                row.getString("merchant"),
                row.getString("idempotency_key"),
                AuthorizationStatus.valueOf(row.getString("status")),
                declineReason == null ? null : DeclineReason.valueOf(declineReason),
                Columns.instant(row, "created_at"),
                Columns.instant(row, "expires_at"));
    }
}
