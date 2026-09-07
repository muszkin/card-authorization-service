package pl.fairydeck.authorization.application.port.out;

import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;

public interface RiskScorer {

    RiskAssessment assess(Purchase purchase);
}
