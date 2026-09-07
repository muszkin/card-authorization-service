package pl.fairydeck.authorization.application;

import org.springframework.stereotype.Service;
import pl.fairydeck.authorization.application.port.out.RiskScorer;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;
import pl.fairydeck.authorization.domain.card.Card;

/**
 * Synchronous authorization of a purchase: find the card (cache-aside), ask the risk engine, then book the
 * decision in one transaction. The risk call is deliberately outside that transaction.
 */
@Service
public class AuthorizePurchase {

    private final CardLookup cards;
    private final RiskScorer riskScorer;
    private final AuthorizationBooking booking;

    public AuthorizePurchase(CardLookup cards, RiskScorer riskScorer, AuthorizationBooking booking) {
        this.cards = cards;
        this.riskScorer = riskScorer;
        this.booking = booking;
    }

    public Authorization authorize(Purchase purchase) {
        Card card = cards.find(purchase.cardId()).orElseThrow(() -> new CardNotFoundException(purchase.cardId()));
        RiskAssessment risk = riskScorer.assess(purchase);
        return booking.book(purchase, card, risk);
    }
}
