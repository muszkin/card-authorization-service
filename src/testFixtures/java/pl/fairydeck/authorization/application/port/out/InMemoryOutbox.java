package pl.fairydeck.authorization.application.port.out;

import java.util.ArrayList;
import java.util.List;
import pl.fairydeck.authorization.domain.authorization.AuthorizationEvent;

public final class InMemoryOutbox implements Outbox {

    private final List<AuthorizationEvent> recorded = new ArrayList<>();

    @Override
    public void record(AuthorizationEvent event) {
        recorded.add(event);
    }

    public List<AuthorizationEvent> recorded() {
        return List.copyOf(recorded);
    }
}
