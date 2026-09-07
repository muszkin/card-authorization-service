package pl.fairydeck.authorization.adapter.out.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cache.cards")
record CardCacheProperties(Duration timeToLive) {
}
