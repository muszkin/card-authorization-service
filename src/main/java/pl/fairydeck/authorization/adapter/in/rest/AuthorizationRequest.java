package pl.fairydeck.authorization.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.money.Money;

record AuthorizationRequest(
        @NotNull UUID cardId,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotBlank String merchant) {

    Purchase toPurchase(String idempotencyKey) {
        Money money = Money.of(amount.toPlainString(), Currency.getInstance(currency));
        return new Purchase(cardId, money, merchant, idempotencyKey);
    }
}
