package pl.fairydeck.authorization.domain.authorization;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.ledger.Balance;
import pl.fairydeck.authorization.domain.money.Money;

/**
 * The decision rules for a purchase, in the order they are applied: card status, available balance, risk.
 * A missing risk score declines the purchase (fail-closed): in regulated credit an unverified approval costs
 * more than a retried decline.
 */
public record AuthorizationPolicy(int maxAcceptableRiskScore, Duration holdValidity) {

    public Authorization authorize(Purchase purchase, Card card, Balance balance, RiskAssessment risk, Instant now) {
        return declineReasonFor(purchase.amount(), card, balance, risk)
                .map(reason -> Authorization.declined(purchase, reason, now))
                .orElseGet(() -> Authorization.approved(purchase, now, holdValidity));
    }

    private Optional<DeclineReason> declineReasonFor(Money amount, Card card, Balance balance, RiskAssessment risk) {
        if (!card.isActive()) {
            return Optional.of(DeclineReason.CARD_NOT_ACTIVE);
        }
        if (balance.available().isLessThan(amount)) {
            return Optional.of(DeclineReason.INSUFFICIENT_FUNDS);
        }
        return switch (risk) {
            case RiskAssessment.Unavailable _ -> Optional.of(DeclineReason.RISK_UNAVAILABLE);
            case RiskAssessment.Scored scored when scored.score() > maxAcceptableRiskScore ->
                    Optional.of(DeclineReason.HIGH_RISK);
            case RiskAssessment.Scored _ -> Optional.empty();
        };
    }
}
