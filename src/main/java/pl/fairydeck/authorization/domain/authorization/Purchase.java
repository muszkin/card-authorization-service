package pl.fairydeck.authorization.domain.authorization;

import java.util.Objects;
import java.util.UUID;
import pl.fairydeck.authorization.domain.money.Money;

public record Purchase(UUID cardId, Money amount, String merchant, String idempotencyKey) {

    public Purchase {
        Objects.requireNonNull(cardId, "cardId");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Purchase amount must be positive");
        }
        requireText(merchant, "Merchant");
        requireText(idempotencyKey, "Idempotency key");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
