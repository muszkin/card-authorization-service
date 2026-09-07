package pl.fairydeck.authorization.adapter.in.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.authorization.DeclineReason;

record AuthorizationResponse(
        UUID id,
        UUID cardId,
        BigDecimal amount,
        String currency,
        String merchant,
        AuthorizationStatus status,
        @Nullable DeclineReason declineReason,
        Instant createdAt,
        @Nullable Instant expiresAt) {

    static AuthorizationResponse from(Authorization authorization) {
        return new AuthorizationResponse(
                authorization.id(),
                authorization.cardId(),
                authorization.amount().amount(),
                authorization.amount().currency().getCurrencyCode(),
                authorization.merchant(),
                authorization.status(),
                authorization.declineReason().orElse(null),
                authorization.createdAt(),
                authorization.expiresAt().orElse(null));
    }
}
