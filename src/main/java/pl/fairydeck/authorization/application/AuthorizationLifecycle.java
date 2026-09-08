package pl.fairydeck.authorization.application;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.fairydeck.authorization.application.port.out.AuthorizationRepository;
import pl.fairydeck.authorization.application.port.out.LedgerRepository;
import pl.fairydeck.authorization.application.port.out.Outbox;
import pl.fairydeck.authorization.domain.authorization.Authorization;
import pl.fairydeck.authorization.domain.authorization.AuthorizationEvent;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;

/**
 * What happens to an approved authorization after the purchase: capture, reversal, or expiry when neither came
 * in time. Each transition is one transaction that appends the ledger entries, updates the status and leaves
 * an event in the outbox.
 */
@Service
public class AuthorizationLifecycle {

    private static final Set<AuthorizationStatus> CAPTURE_ALREADY_SATISFIED = EnumSet.of(AuthorizationStatus.CAPTURED);
    private static final Set<AuthorizationStatus> REVERSE_ALREADY_SATISFIED =
            EnumSet.of(AuthorizationStatus.REVERSED, AuthorizationStatus.EXPIRED);
    private static final Set<AuthorizationStatus> NEVER_ALREADY_SATISFIED = EnumSet.noneOf(AuthorizationStatus.class);

    private final AuthorizationRepository authorizations;
    private final LedgerRepository ledger;
    private final Outbox outbox;
    private final Clock clock;

    public AuthorizationLifecycle(AuthorizationRepository authorizations, LedgerRepository ledger, Outbox outbox,
            Clock clock) {
        this.authorizations = authorizations;
        this.ledger = ledger;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public Authorization capture(UUID authorizationId) {
        return settle(authorizationId, Authorization::capture, CAPTURE_ALREADY_SATISFIED);
    }

    @Transactional
    public Authorization reverse(UUID authorizationId) {
        return settle(authorizationId, Authorization::reverse, REVERSE_ALREADY_SATISFIED);
    }

    /** The compensating step of the hold saga: money nobody claimed goes back to the cardholder. */
    @Scheduled(fixedDelayString = "${authorization.expiry-sweep-interval}")
    @Transactional
    public void releaseExpiredHolds() {
        authorizations.findExpiredHolds(clock.instant())
                .forEach(expired -> settle(expired.id(), Authorization::expire, NEVER_ALREADY_SATISFIED));
    }

    private Authorization settle(UUID authorizationId,
            BiFunction<Authorization, Instant, List<LedgerEntry>> transition, Set<AuthorizationStatus> alreadySatisfiedBy) {
        ledger.lock(load(authorizationId).cardId());
        Authorization authorization = load(authorizationId);
        if (alreadySatisfiedBy.contains(authorization.status())) {
            return authorization;
        }
        Instant now = clock.instant();
        transition.apply(authorization, now).forEach(ledger::append);
        authorizations.save(authorization);
        outbox.record(AuthorizationEvent.of(authorization, now));
        return authorization;
    }

    /** Called twice on purpose: the copy read before the lock may already be stale once the lock is granted. */
    private Authorization load(UUID authorizationId) {
        return authorizations.findById(authorizationId)
                .orElseThrow(() -> new AuthorizationNotFoundException(authorizationId));
    }
}
