package pl.fairydeck.authorization.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.money.Money;

/**
 * A Redis outage must degrade the cache to "always miss", never take the authorization path down with it.
 */
class RedisCardCacheOutageTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Card> redis = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Card> values = mock(ValueOperations.class);
    private final RedisCardCache cache = new RedisCardCache(redis, new CardCacheProperties(Duration.ofMinutes(10)));
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE,
            Money.of("100.00", Currency.getInstance("GBP")));

    @BeforeEach
    void redisIsDown() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("connection refused"));
        doThrow(new RedisConnectionFailureException("connection refused"))
                .when(values).set(anyString(), any(Card.class), any(Duration.class));
    }

    @Test
    void treatsAnUnreachableCacheAsAMiss() {
        assertThat(cache.find(card.id())).isEmpty();
    }

    @Test
    void doesNotFailTheCallerWhenStoringIsImpossible() {
        assertThatCode(() -> cache.store(card)).doesNotThrowAnyException();
    }
}
