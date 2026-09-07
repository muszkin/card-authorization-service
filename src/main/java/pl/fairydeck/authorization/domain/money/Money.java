package pl.fairydeck.authorization.domain.money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Monetary amount stored as minor units of its currency. The exponent comes from the currency itself
 * (GBP has 2 fraction digits, JPY 0, BHD 3), so no code path may assume "times one hundred".
 */
public record Money(long minorUnits, Currency currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money of(String amount, Currency currency) {
        BigDecimal decimal = new BigDecimal(amount);
        int exponent = currency.getDefaultFractionDigits();
        if (decimal.stripTrailingZeros().scale() > exponent) {
            throw new IllegalArgumentException(
                    "Amount %s has more fraction digits than %s allows".formatted(amount, currency));
        }
        return new Money(decimal.movePointRight(exponent).longValueExact(), currency);
    }

    public static Money ofMinorUnits(long minorUnits, Currency currency) {
        return new Money(minorUnits, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    public BigDecimal amount() {
        return BigDecimal.valueOf(minorUnits, currency.getDefaultFractionDigits());
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(minorUnits, sameCurrency(other).minorUnits), currency);
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(minorUnits, sameCurrency(other).minorUnits), currency);
    }

    public boolean isLessThan(Money other) {
        return minorUnits < sameCurrency(other).minorUnits;
    }

    public boolean isPositive() {
        return minorUnits > 0;
    }

    private Money sameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
        return other;
    }
}
