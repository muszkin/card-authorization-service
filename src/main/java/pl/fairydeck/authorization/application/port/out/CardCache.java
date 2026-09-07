package pl.fairydeck.authorization.application.port.out;

import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.card.Card;

public interface CardCache {

    Optional<Card> find(UUID cardId);

    void store(Card card);
}
