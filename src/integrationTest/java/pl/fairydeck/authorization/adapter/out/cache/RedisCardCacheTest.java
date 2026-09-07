package pl.fairydeck.authorization.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Currency;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import pl.fairydeck.authorization.IntegrationTest;
import pl.fairydeck.authorization.application.port.out.CardCache;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.money.Money;

@IntegrationTest
class RedisCardCacheTest {

    private final CardCache cache;
    private final StringRedisTemplate redis;
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE,
            Money.of("1234.5", Currency.getInstance("BHD")));

    RedisCardCacheTest(CardCache cache, StringRedisTemplate redis) {
        this.cache = cache;
        this.redis = redis;
    }

    @Test
    void remembersACardAndFindsItAgainUnchanged() {
        cache.store(card);

        assertThat(cache.find(card.id())).contains(card);
    }

    @Test
    void missesCardsItHasNeverSeen() {
        assertThat(cache.find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void expiresEntriesInsteadOfKeepingThemForever() {
        cache.store(card);

        assertThat(redis.getExpire(RedisCardCache.keyFor(card.id()), TimeUnit.SECONDS)).isPositive();
    }
}
