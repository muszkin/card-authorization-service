package pl.fairydeck.authorization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import pl.fairydeck.authorization.IntegrationTest;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.application.port.out.LedgerRepository;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.ledger.Balance;
import pl.fairydeck.authorization.domain.money.Money;

/**
 * The lost-update scenario: many purchases on one card at the same instant, each individually within the limit,
 * together far beyond it. Whatever the guard is, the ledger must never show more held than the limit allows.
 */
@IntegrationTest
class ConcurrentAuthorizationsTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final int ATTEMPTS = 16;
    private static final Money CREDIT_LIMIT = Money.of("100.00", GBP);
    private static final Money EACH = Money.of("10.00", GBP);
    private static final long AFFORDABLE = CREDIT_LIMIT.minorUnits() / EACH.minorUnits();

    private final AuthorizationBooking booking;
    private final CardRepository cards;
    private final LedgerRepository ledger;

    ConcurrentAuthorizationsTest(AuthorizationBooking booking, CardRepository cards, LedgerRepository ledger) {
        this.booking = booking;
        this.cards = cards;
        this.ledger = ledger;
    }

    @Test
    void concurrentPurchasesCannotOverdrawTheAvailableBalance() throws InterruptedException {
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, CREDIT_LIMIT);
        cards.save(card);

        List<Authorization> decisions = bookAllAtOnce(card);

        Balance balance = Balance.derive(CREDIT_LIMIT, ledger.entriesFor(card.id()));
        assertThat(decisions).filteredOn(Authorization::isApproved).hasSize((int) AFFORDABLE);
        assertThat(balance.available().minorUnits()).isNotNegative();
        assertThat(balance.pending()).isEqualTo(CREDIT_LIMIT);
    }

    private List<Authorization> bookAllAtOnce(Card card) throws InterruptedException {
        CountDownLatch go = new CountDownLatch(1);
        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Authorization>> results = IntStream.range(0, ATTEMPTS)
                    .mapToObj(attempt -> threads.submit(() -> {
                        go.await();
                        return booking.book(purchase(card, attempt), card, new RiskAssessment.Scored(0));
                    }))
                    .toList();
            go.countDown();
            return results.stream().map(ConcurrentAuthorizationsTest::outcome).toList();
        }
    }

    private static Purchase purchase(Card card, int attempt) {
        return new Purchase(card.id(), EACH, "Merchant " + attempt, "race-" + card.id() + "-" + attempt);
    }

    private static Authorization outcome(Future<Authorization> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }
}
