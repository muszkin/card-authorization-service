package pl.fairydeck.authorization.application.port.out;

import java.util.ArrayList;
import java.util.List;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;

public final class StubRiskScorer implements RiskScorer {

    private static final RiskAssessment NO_RISK = new RiskAssessment.Scored(0);

    private final List<Purchase> asked = new ArrayList<>();
    private RiskAssessment answer = NO_RISK;

    public void willAnswer(RiskAssessment assessment) {
        this.answer = assessment;
    }

    @Override
    public RiskAssessment assess(Purchase purchase) {
        asked.add(purchase);
        return answer;
    }

    public List<Purchase> asked() {
        return List.copyOf(asked);
    }
}
