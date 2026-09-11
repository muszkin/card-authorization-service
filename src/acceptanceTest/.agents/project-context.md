<!-- BEGIN project-context-initializer:context -->
# acceptance suite

Path `src/acceptanceTest` | source `5debbf6` (affected scope) | refreshed 2026-09-11T09:42:24Z | coverage: own (real-surface E2E harness)

**Responsibilities.** Cucumber scenarios over HTTP against the whole application:
`resources/pl/fairydeck/authorization/acceptance/card_authorization.feature` (12 scenarios: approval, insufficient
funds, blocked card, idempotent retry, 16 concurrent purchases, risk timeout with a < 1000 ms decision, capture,
reversal, transaction listing), `java/.../acceptance/AcceptanceScenarios.java` (JUnit Platform Suite, glue
`pl.fairydeck.authorization.acceptance`), `AcceptanceContext.java` (`@CucumberContextConfiguration`,
`@SpringBootTest(RANDOM_PORT)`, Testcontainers import, WireMock risk engine via `@DynamicPropertySource`),
`CardAuthorizationSteps.java` (plain `RestClient`, scenario-scoped state, virtual threads for the race).

**Rolled-up.** `java/pl/fairydeck/authorization/acceptance`, `resources/pl/fairydeck/authorization/acceptance`.

**Dependencies.** Gradle suite `acceptanceTest` (cucumber-bom 7.34.8, junit-platform-suite, wiremock-standalone
3.13.2, testFixtures); needs Docker.

**Command.** `./gradlew acceptanceTest` (verified post-review at `5debbf6`: 12 passed), including the numeric-boundary
HTTP behaviour.

**Invariants.** Scenarios speak only to the public API, except issuing a blocked card through `CardRepository`
(no endpoint exists); the risk engine is reset before every scenario.

**Risks.** Timing assertion (< 1000 ms) is generous on purpose; still environment-sensitive on very slow CI.

**Evidence.** files above, `build.gradle.kts` (suite definition), `README.md` "Tests".
<!-- END project-context-initializer:context -->
