package pl.fairydeck.authorization.application.port.out;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.authorization.Authorization;

public final class InMemoryAuthorizationRepository implements AuthorizationRepository {

    private final Map<UUID, Authorization> authorizations = new LinkedHashMap<>();

    @Override
    public void save(Authorization authorization) {
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
}
