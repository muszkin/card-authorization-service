package pl.fairydeck.authorization.acceptance;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.fairydeck.authorization.TestcontainersConfiguration;

/**
 * The complete application on a random port, with Postgres and Redis in containers and the risk engine
 * played by WireMock.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public class AcceptanceContext {

    static final WireMockServer RISK_ENGINE = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @DynamicPropertySource
    static void pointTheApplicationAtTheStubbedRiskEngine(DynamicPropertyRegistry registry) {
        if (!RISK_ENGINE.isRunning()) {
            RISK_ENGINE.start();
        }
        registry.add("risk.scoring.base-url", RISK_ENGINE::baseUrl);
    }
}
