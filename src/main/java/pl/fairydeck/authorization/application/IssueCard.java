package pl.fairydeck.authorization.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.money.Money;

@Service
public class IssueCard {

    private final CardRepository cards;

    public IssueCard(CardRepository cards) {
        this.cards = cards;
    }

    public Card issue(UUID cardholderId, Money creditLimit) {
        Card card = Card.issue(cardholderId, creditLimit);
        cards.save(card);
        return card;
    }
}
