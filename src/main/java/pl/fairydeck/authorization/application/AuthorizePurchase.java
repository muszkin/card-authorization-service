package pl.fairydeck.authorization.application;

import org.springframework.stereotype.Service;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.DuplicateIdempotencyKeyException;
import pl.fairydeck.authorization.application.port.out.RiskScorer;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;
import pl.fairydeck.authorization.domain.card.Card;

/**
 * Synchronous authorization of a purchase: replay a known idempotency key, otherwise find the card
 * (cache-aside), ask the risk engine, then book the decision in one transaction. The risk call is
 * deliberately outside that transaction. Two identical requests racing past the replay check are settled by
 * the store's unique key: the loser returns the winner's decision.
 */
@Service
public class AuthorizePurchase {

    private final CardLookup cards;
    private final RiskScorer riskScorer;
    private final AuthorizationBooking booking;
    private final AuthorizationRepository authorizations;

    public AuthorizePurchase(CardLookup cards, RiskScorer riskScorer, AuthorizationBooking booking,
            AuthorizationRepository authorizations) {
        this.cards = cards;
        this.riskScorer = riskScorer;
        this.booking = booking;
        this.authorizations = authorizations;
    }

    public Authorization authorize(Purchase purchase) {
        return authorizations.findByIdempotencyKey(purchase.idempotencyKey())
                .map(previous -> replay(previous, purchase))
                .orElseGet(() -> authorizeNew(purchase));
    }

    private Authorization authorizeNew(Purchase purchase) {
        Card card = cards.find(purchase.cardId()).orElseThrow(() -> new CardNotFoundException(purchase.cardId()));
        RiskAssessment risk = riskScorer.assess(purchase);
        try {
            return booking.book(purchase, card, risk);
        } catch (DuplicateIdempotencyKeyException raced) {
            return authorizations.findByIdempotencyKey(purchase.idempotencyKey())
                    .map(previous -> replay(previous, purchase))
                    .orElseThrow(() -> raced);
        }
    }

    private static Authorization replay(Authorization previous, Purchase purchase) {
        if (!previous.isFor(purchase)) {
            throw new IdempotencyKeyReusedException(purchase.idempotencyKey());
        }
        return previous;
    }
}
