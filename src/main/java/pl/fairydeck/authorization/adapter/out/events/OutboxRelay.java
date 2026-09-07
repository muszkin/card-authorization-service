package pl.fairydeck.authorization.adapter.out.events;

import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves events from the outbox to the publisher. Publishing is at-least-once: a crash between publish and
 * mark leaves the event pending and it is published again, which is why events carry their own id.
 */
@Component
public class OutboxRelay {

    private final JdbcOutbox outbox;
    private final EventPublisher publisher;
    private final int batchSize;

    OutboxRelay(JdbcOutbox outbox, EventPublisher publisher, OutboxProperties properties) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.batchSize = properties.batchSize();
    }

    @Scheduled(fixedDelayString = "${outbox.relay-interval}")
    @Transactional
    public int relayPendingEvents() {
        List<OutboxEvent> pending = outbox.lockPending(batchSize);
        for (OutboxEvent event : pending) {
            publisher.publish(event);
            outbox.markPublished(event.id());
        }
        return pending.size();
    }
}
