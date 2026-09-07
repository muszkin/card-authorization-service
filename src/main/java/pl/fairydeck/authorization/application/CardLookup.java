package pl.fairydeck.authorization.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pl.fairydeck.authorization.application.port.out.CardCache;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.domain.card.Card;

/**
 * Cache-aside read of a card: serve from the cache, otherwise load from the store and remember the answer.
 * Misses are not cached, so an unknown card id cannot shadow a card created a moment later.
 */
@Service
public class CardLookup {

    private final CardCache cache;
    private final CardRepository cards;

    public CardLookup(CardCache cache, CardRepository cards) {
        this.cache = cache;
        this.cards = cards;
    }

    public Optional<Card> find(UUID cardId) {
        return cache.find(cardId).or(() -> loadAndRemember(cardId));
    }

    private Optional<Card> loadAndRemember(UUID cardId) {
        Optional<Card> card = cards.findById(cardId);
        card.ifPresent(cache::store);
        return card;
    }
}
