package pl.fairydeck.authorization.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.authorization.Authorization;

public interface AuthorizationRepository {

    void save(Authorization authorization);

    Optional<Authorization> findById(UUID id);

    Optional<Authorization> findByIdempotencyKey(String idempotencyKey);

    /** Approved authorizations whose hold validity has passed, oldest first, in a bounded batch. */
    List<Authorization> findExpiredHolds(Instant now);
}
