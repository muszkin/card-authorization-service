package pl.fairydeck.authorization.application;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.fairydeck.authorization.domain.authorization.AuthorizationPolicy;

@Configuration(proxyBeanMethods = false)
class AuthorizationConfiguration {

    @Bean
    Clock clock() {
        return Clock.tickMillis(ZoneOffset.UTC);
    }

    @Bean
    AuthorizationPolicy authorizationPolicy(AuthorizationProperties properties) {
        return new AuthorizationPolicy(properties.maxAcceptableRiskScore(), properties.holdValidity());
    }
}
