package pl.fairydeck.authorization.domain.authorization;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;
import pl.fairydeck.authorization.domain.money.Money;

public final class Authorization {

    private final UUID id;
    private final UUID cardId;
    private final Money amount;
    private final String merchant;
    private final String idempotencyKey;
    private final AuthorizationStatus status;
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

    public LedgerEntry hold() {
        requireStatus(AuthorizationStatus.APPROVED, "place a hold");
        return new LedgerEntry(UUID.randomUUID(), cardId, LedgerEntryType.HOLD, amount, id, createdAt);
    }

    private void requireStatus(AuthorizationStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("Cannot %s on a %s authorization".formatted(action, status));
        }
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
