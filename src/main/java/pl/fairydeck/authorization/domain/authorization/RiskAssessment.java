package pl.fairydeck.authorization.domain.authorization;

/**
 * Outcome of asking the risk engine about a purchase. Unavailability is a first-class outcome rather than an
 * exception, because the policy has to decide what an absent score means.
 */
public sealed interface RiskAssessment {

    record Scored(int score) implements RiskAssessment {
    }

    record Unavailable() implements RiskAssessment {
    }
}
