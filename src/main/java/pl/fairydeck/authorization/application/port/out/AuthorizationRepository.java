package pl.fairydeck.authorization.application.port.out;

import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.authorization.Authorization;

public interface AuthorizationRepository {

    void save(Authorization authorization);

    Optional<Authorization> findById(UUID id);

    Optional<Authorization> findByIdempotencyKey(String idempotencyKey);
}
