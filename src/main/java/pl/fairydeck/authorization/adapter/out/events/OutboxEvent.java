package pl.fairydeck.authorization.adapter.out.events;

import java.util.UUID;

/** An event as it sits in the outbox table: identity, routing type and the serialized payload. */
record OutboxEvent(UUID id, String type, String payload) {
}
