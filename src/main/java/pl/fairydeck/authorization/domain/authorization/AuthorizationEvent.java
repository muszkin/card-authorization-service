package pl.fairydeck.authorization.domain.authorization;

import java.time.Instant;
import java.util.UUID;
import pl.fairydeck.authorization.domain.money.Money;

/**
 * What the rest of the world gets to know about an authorization: which one, on which card, in which state, for
 * how much. Emitted for every state an authorization enters.
 */
public record AuthorizationEvent(
        UUID id,
        UUID authorizationId,
        UUID cardId,
        AuthorizationStatus status,
        Money amount,
        Instant occurredAt) {

    public static AuthorizationEvent of(Authorization authorization, Instant occurredAt) {
        return new AuthorizationEvent(UUID.randomUUID(), authorization.id(), authorization.cardId(),
                authorization.status(), authorization.amount(), occurredAt);
    }
}
