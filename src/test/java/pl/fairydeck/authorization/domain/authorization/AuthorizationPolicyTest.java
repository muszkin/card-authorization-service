package pl.fairydeck.authorization.domain.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pl.fairydeck.authorization.domain.card.Card;
import pl.fairydeck.authorization.domain.card.CardStatus;
import pl.fairydeck.authorization.domain.ledger.Balance;
import pl.fairydeck.authorization.domain.ledger.LedgerEntry;
import pl.fairydeck.authorization.domain.ledger.LedgerEntryType;
import pl.fairydeck.authorization.domain.money.Money;

class AuthorizationPolicyTest {

    private static final Currency GBP = Currency.getInstance("GBP");
    private static final Instant NOW = Instant.parse("2026-09-07T10:00:00Z");
    private static final Duration HOLD_VALIDITY = Duration.ofDays(7);
    private static final int MAX_RISK_SCORE = 70;
    private static final RiskAssessment LOW_RISK = new RiskAssessment.Scored(10);

    private final AuthorizationPolicy policy = new AuthorizationPolicy(MAX_RISK_SCORE, HOLD_VALIDITY);
    private final Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), CardStatus.ACTIVE, gbp("100.00"));
    private final Balance untouchedLimit = Balance.derive(card.creditLimit(), List.of());

    @Test
    void approvesAPurchaseWithinTheAvailableBalance() {
        Authorization authorization = policy.authorize(purchase("30.00"), card, untouchedLimit, LOW_RISK, NOW);

        assertThat(authorization.status()).isEqualTo(AuthorizationStatus.APPROVED);
        assertThat(authorization.declineReason()).isEmpty();
        assertThat(authorization.cardId()).isEqualTo(card.id());
        assertThat(authorization.amount()).isEqualTo(gbp("30.00"));
        assertThat(authorization.createdAt()).isEqualTo(NOW);
        assertThat(authorization.expiresAt()).contains(NOW.plus(HOLD_VALIDITY));
    }

    @Test
    void approvesAPurchaseThatUsesTheWholeAvailableBalance() {
        Authorization authorization = policy.authorize(purchase("100.00"), card, untouchedLimit, LOW_RISK, NOW);

        assertThat(authorization.status()).isEqualTo(AuthorizationStatus.APPROVED);
    }

    @Test
    void declinesAPurchaseExceedingTheAvailableBalance() {
        Authorization authorization = policy.authorize(purchase("100.01"), card, untouchedLimit, LOW_RISK, NOW);

        assertThat(authorization.status()).isEqualTo(AuthorizationStatus.DECLINED);
        assertThat(authorization.declineReason()).contains(DeclineReason.INSUFFICIENT_FUNDS);
        assertThat(authorization.expiresAt()).isEmpty();
    }

    @Test
    void countsExistingHoldsAgainstTheAvailableBalance() {
        Balance withHold = Balance.derive(card.creditLimit(), List.of(hold("80.00")));

        Authorization authorization = policy.authorize(purchase("30.00"), card, withHold, LOW_RISK, NOW);

        assertThat(authorization.declineReason()).contains(DeclineReason.INSUFFICIENT_FUNDS);
    }

    @ParameterizedTest
    @EnumSource(value = CardStatus.class, names = {"BLOCKED", "EXPIRED"})
    void declinesWhenTheCardIsNotActive(CardStatus status) {
        Card inactiveCard = new Card(card.id(), card.cardholderId(), status, card.creditLimit());

        Authorization authorization = policy.authorize(purchase("1.00"), inactiveCard, untouchedLimit, LOW_RISK, NOW);

        assertThat(authorization.status()).isEqualTo(AuthorizationStatus.DECLINED);
        assertThat(authorization.declineReason()).contains(DeclineReason.CARD_NOT_ACTIVE);
    }

    @Test
    void declinesWhenTheRiskScoreExceedsTheAcceptableMaximum() {
        RiskAssessment tooRisky = new RiskAssessment.Scored(MAX_RISK_SCORE + 1);
        RiskAssessment borderline = new RiskAssessment.Scored(MAX_RISK_SCORE);

        assertThat(policy.authorize(purchase("1.00"), card, untouchedLimit, tooRisky, NOW).declineReason())
                .contains(DeclineReason.HIGH_RISK);
        assertThat(policy.authorize(purchase("1.00"), card, untouchedLimit, borderline, NOW).status())
                .isEqualTo(AuthorizationStatus.APPROVED);
    }

    @Test
    void declinesWhenTheRiskAssessmentIsUnavailable() {
        Authorization authorization =
                policy.authorize(purchase("1.00"), card, untouchedLimit, new RiskAssessment.Unavailable(), NOW);

        assertThat(authorization.status()).isEqualTo(AuthorizationStatus.DECLINED);
        assertThat(authorization.declineReason()).contains(DeclineReason.RISK_UNAVAILABLE);
    }

    private Purchase purchase(String amount) {
        return new Purchase(card.id(), gbp(amount), "Coffee Corner", "idempotency-key");
    }

    private LedgerEntry hold(String amount) {
        return new LedgerEntry(UUID.randomUUID(), card.id(), LedgerEntryType.HOLD, gbp(amount), UUID.randomUUID(), NOW);
    }

    private static Money gbp(String amount) {
        return Money.of(amount, GBP);
    }
}
