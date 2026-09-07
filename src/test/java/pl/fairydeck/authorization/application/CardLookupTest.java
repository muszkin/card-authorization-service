package pl.fairydeck.authorization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.application.port.out.InMemoryCardCache;
import pl.fairydeck.authorization.application.port.out.InMemoryCardRepository;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.money.Money;

class CardLookupTest {

    private final InMemoryCardCache cache = new InMemoryCardCache();
    private final InMemoryCardRepository store = new InMemoryCardRepository();
    private final CardLookup lookup = new CardLookup(cache, store);
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE,
            Money.of("100.00", Currency.getInstance("GBP")));

    @Test
    void servesACachedCardWithoutAskingTheStore() {
        cache.store(card);

        assertThat(lookup.find(card.id())).contains(card);
        assertThat(store.reads()).isZero();
    }

    @Test
    void fallsBackToTheStoreOnAMissAndRemembersTheAnswer() {
        store.save(card);

        assertThat(lookup.find(card.id())).contains(card);
        assertThat(store.reads()).isEqualTo(1);
        assertThat(cache.find(card.id())).contains(card);
    }

    @Test
    void reportsAnUnknownCardAsEmptyWithoutCachingTheMiss() {
        assertThat(lookup.find(UUID.randomUUID())).isEmpty();
        assertThat(cache.size()).isZero();
    }
}
