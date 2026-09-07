package pl.fairydeck.authorization.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("authorization")
record AuthorizationProperties(int maxAcceptableRiskScore, Duration holdValidity) {
}
