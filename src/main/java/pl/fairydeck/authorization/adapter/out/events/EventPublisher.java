package pl.fairydeck.authorization.adapter.out.events;

/** The seam where a message broker (SNS, Kafka, ...) would be plugged in. */
interface EventPublisher {

    void publish(OutboxEvent event);
}
