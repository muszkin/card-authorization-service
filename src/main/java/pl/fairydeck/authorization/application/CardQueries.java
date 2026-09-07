package pl.fairydeck.authorization.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.LedgerRepository;
import pl.fairydeck.authorization.application.port.out.TransactionFilter;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.ledger.Balance;

/** Read side of a card: the balance is derived from the ledger on every call, never read from a column. */
@Service
public class CardQueries {

    private final CardLookup cards;
    private final LedgerRepository ledger;
    private final AuthorizationRepository authorizations;

    public CardQueries(CardLookup cards, LedgerRepository ledger, AuthorizationRepository authorizations) {
        this.cards = cards;
        this.ledger = ledger;
        this.authorizations = authorizations;
    }

    public Balance balance(UUID cardId) {
        Card card = find(cardId);
        return Balance.derive(card.creditLimit(), ledger.entriesFor(cardId));
    }

    public List<Authorization> transactions(UUID cardId, TransactionFilter filter) {
        find(cardId);
        return authorizations.findByCard(cardId, filter);
    }

    private Card find(UUID cardId) {
        return cards.find(cardId).orElseThrow(() -> new CardNotFoundException(cardId));
    }
}
