package pl.fairydeck.authorization.application.port.out;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import pl.fairydeck.authorization.domain.authorization.AuthorizationStatus;

/** Optional narrowing of a card's transaction list: {@code from} is inclusive, {@code to} exclusive. */
public record TransactionFilter(@Nullable Instant from, @Nullable Instant to, @Nullable AuthorizationStatus status) {

    public static TransactionFilter none() {
        return new TransactionFilter(null, null, null);
    }
}
