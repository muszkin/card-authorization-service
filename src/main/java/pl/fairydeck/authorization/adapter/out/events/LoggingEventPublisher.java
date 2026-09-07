package pl.fairydeck.authorization.adapter.out.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(OutboxEvent event) {
        log.info("Published event {} of type {}: {}", event.id(), event.type(), event.payload());
    }
}
