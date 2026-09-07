package pl.fairydeck.authorization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.application.port.out.InMemoryCardRepository;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.money.Money;

class IssueCardTest {

    private final InMemoryCardRepository cards = new InMemoryCardRepository();
    private final IssueCard issueCard = new IssueCard(cards);

    @Test
    void storesTheNewlyIssuedCard() {
        Card card = issueCard.issue(UUID.randomUUID(), Money.of("500.00", Currency.getInstance("GBP")));

        assertThat(card.status()).isEqualTo(CardStatus.ACTIVE);
        assertThat(cards.findById(card.id())).contains(card);
    }
}
