package pl.fairydeck.authorization.adapter.out.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("risk.scoring")
record RiskScoringProperties(String baseUrl) {
}
