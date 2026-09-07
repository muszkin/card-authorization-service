package pl.fairydeck.authorization.adapter.out.events;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("outbox")
record OutboxProperties(Duration relayInterval, int batchSize) {
}
