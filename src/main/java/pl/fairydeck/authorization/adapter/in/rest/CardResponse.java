package pl.fairydeck.authorization.adapter.in.rest;

import java.math.BigDecimal;
import java.util.UUID;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;

record CardResponse(UUID id, UUID cardholderId, CardStatus status, BigDecimal creditLimit, String currency) {

    static CardResponse from(Card card) {
        return new CardResponse(card.id(), card.cardholderId(), card.status(), card.creditLimit().amount(),
                card.creditLimit().currency().getCurrencyCode());
    }
}
