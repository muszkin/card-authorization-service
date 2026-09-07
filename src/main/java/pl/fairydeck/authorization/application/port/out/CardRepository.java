package pl.fairydeck.authorization.application.port.out;

import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.card.Card;

public interface CardRepository {

    void save(Card card);

    Optional<Card> findById(UUID id);
}
