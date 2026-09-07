package pl.fairydeck.authorization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.application.port.out.InMemoryAuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.InMemoryCardCache;
import pl.fairydeck.authorization.application.port.out.InMemoryCardRepository;
import pl.fairydeck.authorization.application.port.out.InMemoryLedgerRepository;
import pl.fairydeck.authorization.application.port.out.TransactionFilter;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.ledger.Balance;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;
import pl.fairydeck.authorization.domain.money.Money;

class CardQueriesTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Instant NOW = Instant.parse("2026-09-07T10:00:00Z");

    private final InMemoryCardRepository cards = new InMemoryCardRepository();
    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final InMemoryAuthorizationRepository authorizations = new InMemoryAuthorizationRepository();
    private final CardQueries queries = new CardQueries(new CardLookup(new InMemoryCardCache(), cards), ledger, authorizations);
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, gbp("100.00"));

    @Test
    void derivesTheBalanceFromTheLedger() {
        cards.save(card);
        ledger.append(new LedgerEntry(UUID.randomUUID(), card.id(), LedgerEntryType.HOLD, gbp("30.00"), UUID.randomUUID(), NOW));

        Balance balance = queries.balance(card.id());

        assertThat(balance.pending()).isEqualTo(gbp("30.00"));
        assertThat(balance.available()).isEqualTo(gbp("70.00"));
    }

    @Test
    void listsACardsTransactionsNewestFirst() {
        cards.save(card);
        Authorization older = authorization(NOW.minusSeconds(60), "older");
        Authorization newer = authorization(NOW, "newer");
        authorizations.save(older);
        authorizations.save(newer);

        assertThat(queries.transactions(card.id(), TransactionFilter.none())).containsExactly(newer, older);
        assertThat(queries.transactions(card.id(), new TransactionFilter(NOW, null, null))).containsExactly(newer);
    }

    @Test
    void refusesQueriesForAnUnknownCard() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> queries.balance(unknown)).isInstanceOf(CardNotFoundException.class);
        assertThatThrownBy(() -> queries.transactions(unknown, TransactionFilter.none()))
                .isInstanceOf(CardNotFoundException.class);
    }

    private Authorization authorization(Instant createdAt, String idempotencyKey) {
        return new Authorization(UUID.randomUUID(), card.id(), gbp("10.00"), "Coffee Corner", idempotencyKey,
                AuthorizationStatus.APPROVED, null, createdAt, createdAt.plusSeconds(3600));
    }

    private static Money gbp(String amount) {
        return Money.of(amount, GBP);
    }
}
