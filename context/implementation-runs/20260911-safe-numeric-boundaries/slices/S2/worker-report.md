# S2 worker report

Candidate: `5debbf647734dadcae75960f405ba5602a9bc82b` on `fix/safe-numeric-boundaries-s2`.
Regression-test commit: `55841d60a898b4c892aa77b2bea977ef72768f93`.

## Root cause and change

`Money.of` delegated the external decimal conversion to `BigDecimal.longValueExact()`. Its overflow exception is
`ArithmeticException`, but REST input failures are deliberately mapped through `IllegalArgumentException`; both
REST callers therefore bypassed the 400 problem-detail mapping. `Money.of` now catches only that conversion
exception and rethrows `IllegalArgumentException` with the original exception as its cause. Internal `plus` and
`minus` still use, and expose, `ArithmeticException` from `Math.addExact` and `Math.subtractExact`.

The regression commit covers both long bounds, both overflowing sides, preserved internal arithmetic failures,
both MVC endpoints with no use-case interactions, and one real HTTP Cucumber scenario. The scenario verifies a
400 for both POST endpoints and that the established card retains its full balance and no transactions after each
invalid request. `AI_USAGE.md` now distinguishes the initial Claude Code delivery from this recorded Codex repair,
and accurately says the complete seven-skill bundle is vendored under `.claude/skills` without claiming the
bundled snapshots are the current workflow implementation.

## RED evidence

- `./gradlew test --tests '*MoneyTest' --tests '*AuthorizationControllerTest' --tests '*CardControllerTest'`
  exited 1: 47 tests, 4 failed. Both `Money.of` bound-overflow assertions received the wrong exception/cause, and
  both MVC endpoints failed their expected 400 assertion. Raw log:
  `evidence/focused-red.log` SHA-256
  `c8dc1cb248ad604da904387af1228debdc8116b1d74b775b37770a1e407fd2a4`.
- `./gradlew acceptanceTest --rerun-tasks` exited 1: 12 scenarios, 1 failed. `An oversized monetary request is
  rejected without changing a card` failed at the first expected 400, after application, Postgres, Redis and
  WireMock started. Raw log: `evidence/acceptance-red.log` SHA-256
  `686911974a12fba75d951a12326b2d550eb03b13731d0e82ca9d49b5a6e77341`.
- SHA-256 after RED proof: `MoneyTest.java`
  `70834d19e9faacc0d3305b3537352468e47e1ca40611e00243511b572c4e7c08`;
  `AuthorizationControllerTest.java`
  `3615738753ba38a93f52dd4167fc4b2cbeb6faf46f0f1c3bdd9d7f4cca4aa006`;
  `CardControllerTest.java`
  `309c3c913f0ef8092f1c16f0f25d2fa6d4b385318e430cd1d346fbd763960223`;
  `CardAuthorizationSteps.java`
  `1a00d9dc6f95807b812ee8805bcd1b948e686f8957d2c66a284a2dda8918a1b0`;
  feature `22dc48db202a3a75db13faf81d00a7efb311d580cdc955f31ff6e5685cff6290`.

## Candidate gates

- Focused command above: exit 0 (47 tests); `evidence/focused-green.log` SHA-256
  `547f96fd310a3d2ef705126e6f6812c715835b8c9b300565066d641dd6af354b`.
- `./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava
  compileAcceptanceTestJava`: exit 0; `evidence/static-compile.log` SHA-256
  `483b83ff4ebcfbc2f809ae95cb774bd3ef6a0f900c62b02c1e6a30e0cff263cc`.
- `./gradlew test --tests '*ArchitectureTest'`: exit 0 (5 tests); `evidence/architecture.log` SHA-256
  `ca01621f6e1d5921ced1fff534e1a33deb2fe065d64d75770fdcceddc3de87a2`.
- `git diff --check 3587d2f..HEAD` passed. Exactly seven owned files changed; dependency files are unchanged and
  a diff-level scan for common secret markers found no matches. Sonar: NOT_APPLICABLE (not configured).

The required post-review `./gradlew acceptanceTest --rerun-tasks` is intentionally NOT_RUN on the candidate.

## Runtime evidence and handoff

The RED Cucumber JVM used dynamic application port `42643` and Postgres host port `32789`; it started Redis
(`redis:7-alpine`), Postgres (`postgres:17-alpine`) and WireMock on dynamic ports. The test output records
graceful application and datasource shutdown. Immediately afterward, the Testcontainers-label container query
and application-process query returned no owned runtime; unrelated Docker containers were untouched.

The candidate is immutable and ready for independent review. Residual delivery work is the orchestrator-owned
independent review and candidate E2E run; neither was run here.
