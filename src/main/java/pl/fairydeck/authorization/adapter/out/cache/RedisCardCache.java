package pl.fairydeck.authorization.adapter.out.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import pl.fairydeck.authorization.application.port.out.CardCache;
import pl.fairydeck.authorization.domain.card.Card;

/**
 * Cards in Redis with a time to live. A cache failure is logged and treated as a miss: the store of record
 * still answers, only slower.
 */
@Component
class RedisCardCache implements CardCache {

    private static final Logger log = LoggerFactory.getLogger(RedisCardCache.class);
    private static final String KEY_PREFIX = "card:";

    private final RedisTemplate<String, Card> redis;
    private final Duration timeToLive;

    RedisCardCache(RedisTemplate<String, Card> cardRedisTemplate, CardCacheProperties properties) {
        this.redis = cardRedisTemplate;
        this.timeToLive = properties.timeToLive();
    }

    @Override
    public Optional<Card> find(UUID cardId) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(keyFor(cardId)));
        } catch (DataAccessException e) {
            log.warn("Card cache unavailable, falling back to the store: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void store(Card card) {
        try {
            redis.opsForValue().set(keyFor(card.id()), card, timeToLive);
        } catch (DataAccessException e) {
            log.warn("Card cache unavailable, card {} not cached: {}", card.id(), e.getMessage());
        }
    }

    static String keyFor(UUID cardId) {
        return KEY_PREFIX + cardId;
    }
}
