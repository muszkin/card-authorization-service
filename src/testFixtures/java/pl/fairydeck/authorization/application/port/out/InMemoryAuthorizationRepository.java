package pl.fairydeck.authorization.application.port.out;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;

public final class InMemoryAuthorizationRepository implements AuthorizationRepository {

    private final Map<UUID, Authorization> authorizations = new LinkedHashMap<>();

    @Override
    public void save(Authorization authorization) {
        boolean keyTakenByAnother = authorizations.values().stream()
                .anyMatch(other -> other.idempotencyKey().equals(authorization.idempotencyKey())
                        && !other.id().equals(authorization.id()));
        if (keyTakenByAnother) {
            throw new DuplicateIdempotencyKeyException(authorization.idempotencyKey());
        }
        authorizations.put(authorization.id(), authorization);
    }

    @Override
    public Optional<Authorization> findById(UUID id) {
        return Optional.ofNullable(authorizations.get(id));
    }

    @Override
    public Optional<Authorization> findByIdempotencyKey(String idempotencyKey) {
        return authorizations.values().stream()
                .filter(authorization -> authorization.idempotencyKey().equals(idempotencyKey))
                .findFirst();
    }

    @Override
    public List<Authorization> findExpiredHolds(Instant now) {
        return authorizations.values().stream()
                .filter(authorization -> authorization.status() == AuthorizationStatus.APPROVED)
                .filter(authorization -> authorization.expiresAt().map(expiry -> !now.isBefore(expiry)).orElse(false))
                .toList();
    }
}
