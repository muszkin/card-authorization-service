package pl.fairydeck.authorization.adapter.out.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Currency;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import pl.fairydeck.authorization.IntegrationTest;
import pl.fairydeck.authorization.application.AuthorizationBooking;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.money.Money;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class OutboxTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final RiskAssessment NO_RISK = new RiskAssessment.Scored(0);

    private final AuthorizationBooking booking;
    private final CardRepository cards;
    private final AuthorizationRepository authorizations;
    private final OutboxRelay relay;
    private final TransactionTemplate transactions;
    private final JdbcClient jdbc;
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, Money.of("100.00", GBP));

    OutboxTest(AuthorizationBooking booking, CardRepository cards, AuthorizationRepository authorizations,
            OutboxRelay relay, TransactionTemplate transactions, JdbcClient jdbc) {
        this.booking = booking;
        this.cards = cards;
        this.authorizations = authorizations;
        this.relay = relay;
        this.transactions = transactions;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void storeTheCard() {
        cards.save(card);
    }

    @Test
    void writesTheDecisionAndItsEventTogether() {
        Authorization authorization = booking.book(purchase(), card, NO_RISK);

        assertThat(authorizations.findById(authorization.id())).isPresent();
        assertThat(eventsFor(authorization.id())).isEqualTo(1);
    }

    @Test
    void neverLeavesAnEventWithoutItsDecision() {
        AtomicReference<UUID> rolledBack = new AtomicReference<>();

        transactions.executeWithoutResult(status -> {
            rolledBack.set(booking.book(purchase(), card, NO_RISK).id());
            status.setRollbackOnly();
        });

        assertThat(authorizations.findById(rolledBack.get())).isEmpty();
        assertThat(eventsFor(rolledBack.get())).isZero();
    }

    @Test
    void publishesEachPendingEventExactlyOnce(CapturedOutput output) {
        Authorization authorization = booking.book(purchase(), card, NO_RISK);

        relay.relayPendingEvents();
        relay.relayPendingEvents();

        assertThat(StringUtils.countOccurrencesOf(output.getAll(), authorization.id().toString())).isEqualTo(1);
        assertThat(pendingEvents()).isZero();
    }

    private Purchase purchase() {
        return new Purchase(card.id(), Money.of("30.00", GBP), "Coffee Corner", "outbox-" + UUID.randomUUID());
    }

    private long eventsFor(UUID authorizationId) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE authorization_id = :authorizationId")
                .param("authorizationId", authorizationId)
                .query(Long.class)
                .single();
    }

    private long pendingEvents() {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE published_at IS NULL").query(Long.class).single();
    }
}
