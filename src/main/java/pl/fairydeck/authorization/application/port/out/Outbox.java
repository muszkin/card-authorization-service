package pl.fairydeck.authorization.application.port.out;

import pl.fairydeck.authorization.domain.authorization.AuthorizationEvent;

/**
 * Records an event in the same transaction as the state change it describes. Publishing happens later,
 * asynchronously, from the recorded copy.
 */
public interface Outbox {

    void record(AuthorizationEvent event);
}
