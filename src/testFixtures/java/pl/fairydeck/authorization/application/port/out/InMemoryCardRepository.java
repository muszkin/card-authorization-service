package pl.fairydeck.authorization.application.port.out;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.card.Card;

public final class InMemoryCardRepository implements CardRepository {

    private final Map<UUID, Card> cards = new HashMap<>();
    private int reads;

    @Override
    public void save(Card card) {
        cards.put(card.id(), card);
    }

    @Override
    public Optional<Card> findById(UUID id) {
        reads++;
        return Optional.ofNullable(cards.get(id));
    }

    public int reads() {
        return reads;
    }
}
