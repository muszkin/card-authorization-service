package pl.fairydeck.authorization.adapter.out.risk;

import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.fairydeck.authorization.application.port.out.RiskScorer;
import pl.fairydeck.authorization.domain.authorization.Purchase;
import pl.fairydeck.authorization.domain.authorization.RiskAssessment;

/**
 * Asks the external risk engine for a score. Any failure to obtain one, including a timeout, is reported as
 * {@link RiskAssessment.Unavailable}; deciding what that means for the purchase is the domain's job.
 */
@Component
class HttpRiskScorer implements RiskScorer {

    private static final Logger log = LoggerFactory.getLogger(HttpRiskScorer.class);
    private static final String SCORES_PATH = "/v1/scores";

    private final RestClient client;

    HttpRiskScorer(RestClient riskScoringClient) {
        this.client = riskScoringClient;
    }

    @Override
    public RiskAssessment assess(Purchase purchase) {
        try {
            ScoreResponse response = client.post()
                    .uri(SCORES_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ScoreRequest.of(purchase))
                    .retrieve()
                    .body(ScoreResponse.class);
            if (response == null || response.score() == null) {
                return new RiskAssessment.Unavailable();
            }
            try {
                return new RiskAssessment.Scored(response.score().intValueExact());
            } catch (ArithmeticException e) {
                log.warn("Risk scoring returned an inexact score for card {}", purchase.cardId());
                return new RiskAssessment.Unavailable();
            }
        } catch (RestClientException e) {
            log.warn("Risk scoring unavailable for card {}: {}", purchase.cardId(), e.getMessage());
            return new RiskAssessment.Unavailable();
        }
    }

    record ScoreRequest(UUID cardId, BigDecimal amount, String currency, String merchant) {

        static ScoreRequest of(Purchase purchase) {
            return new ScoreRequest(purchase.cardId(), purchase.amount().amount(),
                    purchase.amount().currency().getCurrencyCode(), purchase.merchant());
        }
    }

    record ScoreResponse(BigDecimal score) {
    }
}
