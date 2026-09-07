package pl.fairydeck.authorization.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import pl.fairydeck.authorization.IntegrationTest;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.CardRepository;
import pl.fairydeck.authorization.application.port.out.LedgerRepository;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;
import pl.fairydeck.authorization.domain.money.Money;

@IntegrationTest
@Transactional
class LedgerRepositoryTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Instant NOW = Instant.parse("2026-09-07T10:00:00Z");

    private final LedgerRepository ledger;
    private final CardRepository cards;
    private final AuthorizationRepository authorizations;
    private final JdbcClient jdbc;

    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, Money.of("100.00", GBP));
    private final Authorization authorization = new Authorization(UUID.randomUUID(), card.id(), Money.of("30.00", GBP),
            "Coffee Corner", "idempotency-key", AuthorizationStatus.APPROVED, null, NOW, NOW.plusSeconds(3600));

    LedgerRepositoryTest(LedgerRepository ledger, CardRepository cards, AuthorizationRepository authorizations,
            JdbcClient jdbc) {
        this.ledger = ledger;
        this.cards = cards;
        this.authorizations = authorizations;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void storeTheCardAndItsAuthorization() {
        cards.save(card);
        authorizations.save(authorization);
    }

    @Test
    void appendsEntriesAndReturnsThemInTheOrderTheyWereWritten() {
        LedgerEntry hold = entry(LedgerEntryType.HOLD);
        LedgerEntry release = entry(LedgerEntryType.HOLD_RELEASE);

        ledger.append(hold);
        ledger.append(release);

        assertThat(ledger.entriesFor(card.id())).containsExactly(hold, release);
    }

    @Test
    void keepsTheLedgersOfDifferentCardsApart() {
        ledger.append(entry(LedgerEntryType.HOLD));

        assertThat(ledger.entriesFor(UUID.randomUUID())).isEmpty();
    }

    @Test
    void theDatabaseRefusesToChangeAnEntryOnceWritten() {
        ledger.append(entry(LedgerEntryType.HOLD));

        assertThatThrownBy(() -> jdbc.sql("UPDATE ledger_entries SET amount_minor = 1").update())
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    private LedgerEntry entry(LedgerEntryType type) {
        return new LedgerEntry(UUID.randomUUID(), card.id(), type, Money.of("30.00", GBP), authorization.id(), NOW);
    }
}
