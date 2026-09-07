package pl.fairydeck.authorization.domain.card;

import java.util.Objects;
import java.util.UUID;
import pl.fairydeck.authorization.domain.money.Money;

public record Card(UUID id, UUID cardholderId, CardStatus status, Money creditLimit) {

    public Card {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(cardholderId, "cardholderId");
        Objects.requireNonNull(status, "status");
        if (!creditLimit.isPositive()) {
            throw new IllegalArgumentException("Credit limit must be positive");
        }
    }

    public static Card issue(UUID cardholderId, Money creditLimit) {
        return new Card(UUID.randomUUID(), cardholderId, CardStatus.ACTIVE, creditLimit);
    }

    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }
}
