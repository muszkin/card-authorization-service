package pl.fairydeck.authorization.adapter.out.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.fairydeck.authorization.application.port.out.Outbox;
import pl.fairydeck.authorization.domain.authorization.AuthorizationEvent;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcOutbox implements Outbox {

    private static final String TYPE_PREFIX = "authorization.";

    private final JdbcClient jdbc;
    private final JsonMapper json;

    JdbcOutbox(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void record(AuthorizationEvent event) {
        jdbc.sql("INSERT INTO outbox_events (id, authorization_id, type, payload, occurred_at) "
                        + "VALUES (:id, :authorizationId, :type, CAST(:payload AS jsonb), :occurredAt)")
                .param("id", event.id())
                .param("authorizationId", event.authorizationId())
                .param("type", TYPE_PREFIX + event.status().name().toLowerCase(Locale.ROOT))
                .param("payload", json.writeValueAsString(Payload.of(event)))
                .param("occurredAt", event.occurredAt().atOffset(ZoneOffset.UTC))
                .update();
    }

    /** Pending events in insertion order, locked for this transaction and skipped by any concurrent relay. */
    List<OutboxEvent> lockPending(int limit) {
        return jdbc.sql("SELECT id, type, payload FROM outbox_events WHERE published_at IS NULL "
                        + "ORDER BY position LIMIT :limit FOR UPDATE SKIP LOCKED")
                .param("limit", limit)
                .query((row, rowNumber) -> new OutboxEvent(
                        row.getObject("id", UUID.class), row.getString("type"), row.getString("payload")))
                .list();
    }

    void markPublished(UUID eventId) {
        jdbc.sql("UPDATE outbox_events SET published_at = now() WHERE id = :id").param("id", eventId).update();
    }

    /** The published contract: flat, explicit and independent of how the domain represents money. */
    record Payload(UUID eventId, UUID authorizationId, UUID cardId, String status, BigDecimal amount,
            String currency, Instant occurredAt) {

        static Payload of(AuthorizationEvent event) {
            return new Payload(event.id(), event.authorizationId(), event.cardId(), event.status().name(),
                    event.amount().amount(), event.amount().currency().getCurrencyCode(), event.occurredAt());
        }
    }
}
