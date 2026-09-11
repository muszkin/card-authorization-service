# S1 worker report

Candidate: `3587d2fec48a4956d57bd785840f98d930d750a3` on `fix/safe-numeric-boundaries-s1`.
Regression-test commit: `b21f69c5f711565fe656d922d4f0fa08a9a59366`.

## Root cause and change

Jackson bound the external `70.9` JSON number directly to `int`, truncating it to `70` before the adapter created `RiskAssessment.Scored`.
`HttpRiskScorer` now binds the wire value as `BigDecimal`; a missing value or `intValueExact()` conversion failure returns the existing fail-closed `RiskAssessment.Unavailable`. The conversion catch is limited to `ArithmeticException`; existing REST-client failure handling is unchanged. No score range rule was introduced.

The regression commit adds adapter coverage for `70`, `70.0`, `70.9`, missing/null score, malformed JSON, and integer overflow, plus the full HTTP Cucumber scenario asserting `201 DECLINED/RISK_UNAVAILABLE` and unchanged available balance for `70.9`.

## RED evidence

- `./gradlew test --tests '*HttpRiskScorerTest'` exited 1: 17 tests, one failed (`reportsUnavailabilityWhenTheRiskServiceReturnsANonIntegralScore`); unchanged code returned `Scored(70)`. Raw log: `evidence/focused-red.log`.
- `./gradlew acceptanceTest --rerun-tasks` exited 1: 11 scenarios, one failed (`A fractional risk score declines the purchase without a hold`), because the actual HTTP response was approved. Raw log: `evidence/acceptance-red.log`.
- SHA-256 after the RED proof: `HttpRiskScorerTest.java` `63e230645fd4ecaddd938b83f186e0c7336f4347dee7436dc0655a35bc81356c`; `CardAuthorizationSteps.java` `17e83f4f783a3ceb1e9f270ffd3dae03032f6fae19759c850231b32381567047`; `card_authorization.feature` `678ed041e7eb250dac3d91ee857d884cbcbcdb9e23799ed8375f10f96fd91c1c`.

## Candidate gates

- `./gradlew test --tests '*HttpRiskScorerTest'`: exit 0 (17 tests). Raw log: `evidence/focused-green.log`.
- `./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava compileAcceptanceTestJava`: exit 0. Raw log: `evidence/static-compile.log`.
- `./gradlew test --tests '*ArchitectureTest'`: exit 0 (5 tests). Raw log: `evidence/architecture.log`.
- `git diff --check 73f5ee0..HEAD`: clean. Dependency files unchanged. Diff-level scan for common secret markers produced no matches. Sonar: NOT_APPLICABLE (not configured).

The required post-review `./gradlew acceptanceTest --rerun-tasks` is intentionally NOT_RUN on the candidate.

## Runtime evidence

The RED Cucumber run used dynamic application port `34317`, WireMock `43921`, and Testcontainers host port `32777`, as captured in the JUnit output. After shutdown, no container with `org.testcontainers=true` and no acceptance/application worker process remained. The existing unrelated Docker containers were left untouched.

## Handoff and residual risk

Only the four S1-owned files changed. The candidate is immutable and ready for independent review. Residual delivery work is the required independent review followed by the orchestrator-owned candidate E2E run; neither was performed here.
