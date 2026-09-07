package pl.fairydeck.authorization.domain.money;

import java.util.Currency;

public class CurrencyMismatchException extends IllegalArgumentException {

    public CurrencyMismatchException(Currency left, Currency right) {
        super("Cannot combine %s with %s".formatted(left, right));
    }
}
