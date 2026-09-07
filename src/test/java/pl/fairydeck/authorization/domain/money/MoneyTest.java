package pl.fairydeck.authorization.domain.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Currency JPY = Currency.getInstance("JPY");

    @ParameterizedTest(name = "{0} {1} is {2} minor units")
    @CsvSource({
            "12.34, GBP, 1234",
            "1200, JPY, 1200",
            "1.234, BHD, 1234",
            "5, GBP, 500",
            "0.5, GBP, 50"
    })
    void parsesDecimalAmountUsingTheCurrencyExponent(String amount, String currency, long expectedMinorUnits) {
        Money money = Money.of(amount, Currency.getInstance(currency));

        assertThat(money.minorUnits()).isEqualTo(expectedMinorUnits);
    }

    @ParameterizedTest(name = "{0} {1} has too many fraction digits")
    @CsvSource({
            "12.345, GBP",
            "12.5, JPY",
            "1.2345, BHD"
    })
    void rejectsMoreFractionDigitsThanTheCurrencyAllows(String amount, String currency) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Money.of(amount, Currency.getInstance(currency)))
                .withMessageContaining(currency);
    }

    @ParameterizedTest(name = "{0} minor units of {1} render as {2}")
    @CsvSource({
            "1234, GBP, 12.34",
            "1200, JPY, 1200",
            "1234, BHD, 1.234",
            "5, GBP, 0.05"
    })
    void rendersAsDecimalWithTheCurrencyScale(long minorUnits, String currency, String expected) {
        Money money = Money.ofMinorUnits(minorUnits, Currency.getInstance(currency));

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal(expected));
        assertThat(money.amount().scale()).isEqualTo(Currency.getInstance(currency).getDefaultFractionDigits());
    }

    @Test
    void addsAmountsOfTheSameCurrency() {
        Money sum = Money.of("10.00", GBP).plus(Money.of("2.50", GBP));

        assertThat(sum).isEqualTo(Money.of("12.50", GBP));
    }

    @Test
    void subtractsAmountsOfTheSameCurrency() {
        Money difference = Money.of("10.00", GBP).minus(Money.of("2.50", GBP));

        assertThat(difference).isEqualTo(Money.of("7.50", GBP));
    }

    @Test
    void refusesArithmeticAcrossCurrencies() {
        Money pounds = Money.of("10.00", GBP);
        Money yen = Money.of("1000", JPY);

        assertThatThrownBy(() -> pounds.plus(yen))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("GBP")
                .hasMessageContaining("JPY");
    }

    @Test
    void comparesAmountsOfTheSameCurrency() {
        Money smaller = Money.of("9.99", GBP);
        Money larger = Money.of("10.00", GBP);

        assertThat(smaller.isLessThan(larger)).isTrue();
        assertThat(larger.isLessThan(smaller)).isFalse();
        assertThat(larger.isLessThan(larger)).isFalse();
    }

    @Test
    void knowsWhetherItIsPositive() {
        assertThat(Money.of("0.01", GBP).isPositive()).isTrue();
        assertThat(Money.zero(GBP).isPositive()).isFalse();
        assertThat(Money.of("-0.01", GBP).isPositive()).isFalse();
    }
}
