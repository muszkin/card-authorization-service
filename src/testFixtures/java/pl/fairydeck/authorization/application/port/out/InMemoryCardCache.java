package pl.fairydeck.authorization.application.port.out;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.card.Card;

public final class InMemoryCardCache implements CardCache {

    private final Map<UUID, Card> cards = new HashMap<>();

    @Override
    public Optional<Card> find(UUID cardId) {
        return Optional.ofNullable(cards.get(cardId));
    }

    @Override
    public void store(Card card) {
        cards.put(card.id(), card);
    }

    public int size() {
        return cards.size();
    }
}
