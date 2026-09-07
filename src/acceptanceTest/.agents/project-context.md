<!-- BEGIN project-context-initializer:context -->
# acceptance suite

Path `src/acceptanceTest` | source `b4bef16` | refreshed 2026-09-07T14:57:44Z | coverage: own (real-surface E2E harness)

**Responsibilities.** Cucumber scenarios over HTTP against the whole application:
`resources/pl/fairydeck/authorization/acceptance/card_authorization.feature` (9 scenarios: approval, insufficient
funds, blocked card, idempotent retry, 16 concurrent purchases, risk timeout with a < 1000 ms decision, capture,
reversal, transaction listing), `java/.../acceptance/AcceptanceScenarios.java` (JUnit Platform Suite, glue
`pl.fairydeck.authorization.acceptance`), `AcceptanceContext.java` (`@CucumberContextConfiguration`,
`@SpringBootTest(RANDOM_PORT)`, Testcontainers import, WireMock risk engine via `@DynamicPropertySource`),
`CardAuthorizationSteps.java` (plain `RestClient`, scenario-scoped state, virtual threads for the race).

**Rolled-up.** `java/pl/fairydeck/authorization/acceptance`, `resources/pl/fairydeck/authorization/acceptance`.

**Dependencies.** Gradle suite `acceptanceTest` (cucumber-bom 7.34.8, junit-platform-suite, wiremock-standalone
3.13.2, testFixtures); needs Docker.

**Command.** `./gradlew acceptanceTest` (verified at `b4bef16`: 9 passed).

**Invariants.** Scenarios speak only to the public API, except issuing a blocked card through `CardRepository`
(no endpoint exists); the risk engine is reset before every scenario.

**Risks.** Timing assertion (< 1000 ms) is generous on purpose; still environment-sensitive on very slow CI.

**Evidence.** files above, `build.gradle.kts` (suite definition), `README.md` "Tests".
<!-- END project-context-initializer:context -->
