package pl.fairydeck.authorization.domain.authorization;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;
import pl.fairydeck.authorization.domain.money.Money;

/**
 * The lifecycle of one purchase decision. An approved authorization holds money on the card until it is
 * captured, reversed or expires; every transition hands back the ledger entries that record it.
 */
public final class Authorization {

    private final UUID id;
    private final UUID cardId;
    private final Money amount;
    private final String merchant;
    private final String idempotencyKey;
    private AuthorizationStatus status;
    private final DeclineReason declineReason;
    private final Instant createdAt;
    private final Instant expiresAt;

    public Authorization(
            UUID id,
            UUID cardId,
            Money amount,
            String merchant,
            String idempotencyKey,
            AuthorizationStatus status,
            DeclineReason declineReason,
            Instant createdAt,
            Instant expiresAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.cardId = Objects.requireNonNull(cardId, "cardId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.merchant = Objects.requireNonNull(merchant, "merchant");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.status = Objects.requireNonNull(status, "status");
        this.declineReason = declineReason;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = expiresAt;
    }

    static Authorization approved(Purchase purchase, Instant now, Duration holdValidity) {
        return new Authorization(UUID.randomUUID(), purchase.cardId(), purchase.amount(), purchase.merchant(),
                purchase.idempotencyKey(), AuthorizationStatus.APPROVED, null, now, now.plus(holdValidity));
    }

    static Authorization declined(Purchase purchase, DeclineReason reason, Instant now) {
        return new Authorization(UUID.randomUUID(), purchase.cardId(), purchase.amount(), purchase.merchant(),
                purchase.idempotencyKey(), AuthorizationStatus.DECLINED, reason, now, null);
    }

    /** Whether this authorization was created for the given purchase, which is what makes a retry a retry. */
    public boolean isFor(Purchase purchase) {
        return cardId.equals(purchase.cardId())
                && amount.equals(purchase.amount())
                && merchant.equals(purchase.merchant());
    }

    public boolean isApproved() {
        return status == AuthorizationStatus.APPROVED;
    }

    public LedgerEntry hold() {
        requireStatus(AuthorizationStatus.APPROVED, "place a hold");
        return entry(LedgerEntryType.HOLD, createdAt);
    }

    /** Settles the purchase: the hold is released and the same amount is booked as a charge. */
    public List<LedgerEntry> capture(Instant now) {
        requireStatus(AuthorizationStatus.APPROVED, "capture");
        if (!now.isBefore(holdExpiry())) {
            throw new IllegalStateException("Cannot capture: the hold expired at " + holdExpiry());
        }
        status = AuthorizationStatus.CAPTURED;
        return List.of(entry(LedgerEntryType.HOLD_RELEASE, now), entry(LedgerEntryType.CAPTURE, now));
    }

    /** Cancels the purchase: the hold is released and the balance is restored. */
    public List<LedgerEntry> reverse(Instant now) {
        requireStatus(AuthorizationStatus.APPROVED, "reverse");
        status = AuthorizationStatus.REVERSED;
        return List.of(entry(LedgerEntryType.HOLD_RELEASE, now));
    }

    /** Compensates a hold nobody settled in time: released like a reversal, but recorded as expired. */
    public List<LedgerEntry> expire(Instant now) {
        requireStatus(AuthorizationStatus.APPROVED, "expire");
        if (now.isBefore(holdExpiry())) {
            throw new IllegalStateException("Cannot expire: the hold is valid until " + holdExpiry());
        }
        status = AuthorizationStatus.EXPIRED;
        return List.of(entry(LedgerEntryType.HOLD_RELEASE, now));
    }

    private void requireStatus(AuthorizationStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("Cannot %s on a %s authorization".formatted(action, status));
        }
    }

    private Instant holdExpiry() {
        return Objects.requireNonNull(expiresAt, "an approved authorization always has an expiry");
    }

    private LedgerEntry entry(LedgerEntryType type, Instant at) {
        return new LedgerEntry(UUID.randomUUID(), cardId, type, amount, id, at);
    }

    public UUID id() {
        return id;
    }

    public UUID cardId() {
        return cardId;
    }

    public Money amount() {
        return amount;
    }

    public String merchant() {
        return merchant;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public AuthorizationStatus status() {
        return status;
    }

    public Optional<DeclineReason> declineReason() {
        return Optional.ofNullable(declineReason);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }
}
