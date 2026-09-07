package pl.fairydeck.authorization.adapter.in.rest;

import java.math.BigDecimal;
import java.util.UUID;
import pl.fairydeck.authorization.domain.ledger.Balance;

record BalanceResponse(
        UUID cardId,
        String currency,
        BigDecimal creditLimit,
        BigDecimal pending,
        BigDecimal settled,
        BigDecimal available) {

    static BalanceResponse of(UUID cardId, Balance balance) {
        return new BalanceResponse(cardId, balance.creditLimit().currency().getCurrencyCode(),
                balance.creditLimit().amount(), balance.pending().amount(), balance.settled().amount(),
                balance.available().amount());
    }
}
