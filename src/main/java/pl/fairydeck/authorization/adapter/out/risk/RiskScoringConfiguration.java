package pl.fairydeck.authorization.adapter.out.risk;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class RiskScoringConfiguration {

    @Bean
    RestClient riskScoringClient(RestClient.Builder builder, RiskScoringProperties properties) {
        return builder.baseUrl(properties.baseUrl()).build();
    }
}
