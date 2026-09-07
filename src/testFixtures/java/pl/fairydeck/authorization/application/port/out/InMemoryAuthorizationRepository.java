package pl.fairydeck.authorization.application.port.out;

import java.time.Instant;
import java.util.Comparator;
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

    @Override
    public List<Authorization> findByCard(UUID cardId, TransactionFilter filter) {
        return authorizations.values().stream()
                .filter(authorization -> authorization.cardId().equals(cardId))
                .filter(authorization -> filter.from() == null || !authorization.createdAt().isBefore(filter.from()))
                .filter(authorization -> filter.to() == null || authorization.createdAt().isBefore(filter.to()))
                .filter(authorization -> filter.status() == null || authorization.status() == filter.status())
                .sorted(Comparator.comparing(Authorization::createdAt).reversed())
                .toList();
    }
}
