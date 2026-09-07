package pl.fairydeck.authorization.application;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.LedgerRepository;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationPolicy;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.ledger.Balance;

/**
 * The transactional heart of an authorization: lock the card's ledger, derive the balance, decide, and write
 * the decision together with its hold. Everything that talks to the network happens before this class is
 * called, so the transaction never waits on a remote system.
 *
 * <p>READ COMMITTED is chosen on purpose. Once the lock is acquired, the ledger read sees every hold committed
 * by the transactions that held the lock before, because each statement takes a fresh snapshot. Under
 * REPEATABLE READ the snapshot would predate the lock wait and the stale balance would overdraw the card;
 * SERIALIZABLE would catch that but only by aborting and retrying under contention.
 */
@Service
public class AuthorizationBooking {

    private final LedgerRepository ledger;
    private final AuthorizationRepository authorizations;
    private final AuthorizationPolicy policy;
    private final Clock clock;

    public AuthorizationBooking(LedgerRepository ledger, AuthorizationRepository authorizations,
            AuthorizationPolicy policy, Clock clock) {
        this.ledger = ledger;
        this.authorizations = authorizations;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Authorization book(Purchase purchase, Card card, RiskAssessment risk) {
        ledger.lock(card.id());
        Balance balance = Balance.derive(card.creditLimit(), ledger.entriesFor(card.id()));
        Authorization authorization = policy.authorize(purchase, card, balance, risk, clock.instant());
        authorizations.save(authorization);
        if (authorization.isApproved()) {
            ledger.append(authorization.hold());
        }
        return authorization;
    }
}
