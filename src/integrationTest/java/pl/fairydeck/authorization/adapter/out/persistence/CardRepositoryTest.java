package pl.fairydeck.authorization.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import pl.fairydeck.authorization.IntegrationTest;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.money.Money;

@IntegrationTest
@Transactional
class CardRepositoryTest {

    private final CardRepository cards;

    CardRepositoryTest(CardRepository cards) {
        this.cards = cards;
    }

    @Test
    void storesACardAndReadsItBackUnchanged() {
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.BLOCKED,
                Money.of("1234.56", Currency.getInstance("GBP")));

        cards.save(card);

        assertThat(cards.findById(card.id())).contains(card);
    }

    @Test
    void keepsTheCurrencyExponentOfTheStoredLimit() {
        Card yenCard = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE,
                Money.of("50000", Currency.getInstance("JPY")));

        cards.save(yenCard);

        assertThat(cards.findById(yenCard.id())).contains(yenCard);
    }

    @Test
    void readsAnUnknownCardAsEmpty() {
        assertThat(cards.findById(UUID.randomUUID())).isEmpty();
    }
}
