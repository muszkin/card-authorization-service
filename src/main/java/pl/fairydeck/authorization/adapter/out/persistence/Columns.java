package pl.fairydeck.authorization.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import pl.fairydeck.authorization.domain.money.Money;

/**
 * The few column conventions shared by every table: money as minor units plus an ISO currency code, and
 * instants as UTC timestamps.
 */
final class Columns {

    private Columns() {
    }

    static UUID uuid(ResultSet row, String column) throws SQLException {
        return row.getObject(column, UUID.class);
    }

    static Money money(ResultSet row, String amountColumn, String currencyColumn) throws SQLException {
        return Money.ofMinorUnits(row.getLong(amountColumn), Currency.getInstance(row.getString(currencyColumn)));
    }

    static @Nullable Instant instant(ResultSet row, String column) throws SQLException {
        OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    static @Nullable OffsetDateTime timestamp(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
