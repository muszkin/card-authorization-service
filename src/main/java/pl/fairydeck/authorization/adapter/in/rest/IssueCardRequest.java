package pl.fairydeck.authorization.adapter.in.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import pl.fairydeck.authorization.domain.money.Money;

record IssueCardRequest(
        @NotNull UUID cardholderId,
        @NotNull @Positive BigDecimal creditLimit,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency) {

    Money limit() {
        return Money.of(creditLimit.toPlainString(), Currency.getInstance(currency));
    }
}
