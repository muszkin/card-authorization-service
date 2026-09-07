package pl.fairydeck.authorization.application;

import java.util.UUID;

public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(UUID cardId) {
        super("Card %s does not exist".formatted(cardId));
    }
}
