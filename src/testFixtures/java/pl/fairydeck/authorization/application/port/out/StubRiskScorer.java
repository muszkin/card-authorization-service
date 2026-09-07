package pl.fairydeck.authorization.application.port.out;

import java.util.ArrayList;
import java.util.List;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;

public final class StubRiskScorer implements RiskScorer {

    private static final RiskAssessment NO_RISK = new RiskAssessment.Scored(0);

    private final List<Purchase> asked = new ArrayList<>();
    private RiskAssessment answer = NO_RISK;
    private Runnable whileAssessing = () -> { };

    public void willAnswer(RiskAssessment assessment) {
        this.answer = assessment;
    }

    /** Runs while the risk engine is "thinking", to interleave other work with the scoring call. */
    public void whileAssessing(Runnable sideEffect) {
        this.whileAssessing = sideEffect;
    }

    @Override
    public RiskAssessment assess(Purchase purchase) {
        asked.add(purchase);
        whileAssessing.run();
        return answer;
    }

    public List<Purchase> asked() {
        return List.copyOf(asked);
    }
}
