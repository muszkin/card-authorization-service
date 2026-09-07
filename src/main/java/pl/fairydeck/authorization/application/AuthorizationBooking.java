package pl.fairydeck.authorization.application;

import java.time.Clock;
import org.springframework.stereotype.Service;
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
 * The transactional heart of an authorization: derive the balance from the ledger, decide, and write the
 * decision together with its hold. Everything that talks to the network happens before this class is called,
 * so the transaction never waits on a remote system.
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

    @Transactional
    public Authorization book(Purchase purchase, Card card, RiskAssessment risk) {
        Balance balance = Balance.derive(card.creditLimit(), ledger.entriesFor(card.id()));
        Authorization authorization = policy.authorize(purchase, card, balance, risk, clock.instant());
        authorizations.save(authorization);
        if (authorization.isApproved()) {
            ledger.append(authorization.hold());
        }
        return authorization;
    }
}
