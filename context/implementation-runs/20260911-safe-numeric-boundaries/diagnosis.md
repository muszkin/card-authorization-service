# Numeric-boundary diagnosis

Base revision: `d7537cdb2b25fbf6a6b4ec7bed4dbddd8bf274fb`  
Worktree: `diagnostic worktree at d7537cd`  
Baseline: `./gradlew clean build` passed before this investigation (35 s); log: `/tmp/zilch-20260911-baseline.log`.

## Reported symptom F1: fractional external risk score

**Observed / reproduction:** WireMock returned `{"score":70.9}`. The focused adapter regression expects the
fail-closed port result `RiskAssessment.Unavailable`, but the application returned `Scored[score=70]`.
The same test failed on three runs (the combined run plus two focused reruns).

**Nearest control:** `{"score":70.0}` returns `Scored[score=70]` in the same focused suite.

**Minimal command:**

```bash
cd diagnostic worktree at d7537cd
./gradlew test --tests 'pl.fairydeck.authorization.adapter.out.risk.HttpRiskScorerTest'
```

**Evidence:** `/tmp/zilch-20260911-focused-regressions.log`,
`/tmp/zilch-20260911-risk-regression-rerun-1.log`,
`/tmp/zilch-20260911-risk-regression-rerun-2.log`, and raw JUnit XML
`/tmp/zilch-20260911-risk-regression-rerun-2.xml`.

**Mechanism (proven):** `HttpRiskScorer.ScoreResponse` declares `int score`
(`src/main/java/pl/fairydeck/authorization/adapter/out/risk/HttpRiskScorer.java:48`). The HTTP message
converter binds the fractional JSON number to that integer and truncates it before
`new RiskAssessment.Scored(response.score())` is constructed. Thus `70.9` appears as `70`, rather than
being treated as a malformed external response and translated by the existing fail-closed adapter policy.

**Why prior tests missed it:** the contract test used only integral JSON (`42`, `1`), while its unavailable
coverage used delay, 5xx, and 503. No test supplied a successful HTTP response with a fractional score.

## Reported symptom F2: monetary overflow

**Observed / reproduction:** `Money.of("92233720368547758.08", GBP)` fails with
`ArithmeticException: Overflow`, although invalid user input is expected to surface as an
`IllegalArgumentException` and HTTP 400 problem detail. The exact overflow reaches both public request paths:
`POST /v1/authorizations` and `POST /v1/cards`; in each web slice it escapes as
`jakarta.servlet.ServletException: Request processing failed: java.lang.ArithmeticException: Overflow`.

**Nearest control:** `Money.of("92233720368547758.07", GBP)` succeeds and equals
`Money.ofMinorUnits(Long.MAX_VALUE, GBP)`.

**Minimal commands:**

```bash
cd diagnostic worktree at d7537cd
./gradlew test --tests 'pl.fairydeck.authorization.domain.money.MoneyTest'
./gradlew test --tests 'pl.fairydeck.authorization.adapter.in.rest.AuthorizationControllerTest'
./gradlew test --tests 'pl.fairydeck.authorization.adapter.in.rest.CardControllerTest'
```

**Evidence:** `/tmp/zilch-20260911-focused-regressions.log`,
`/tmp/zilch-20260911-money-regression-rerun-1.log`,
`/tmp/zilch-20260911-money-regression-rerun-2.log`,
`/tmp/zilch-20260911-money-regression-rerun-2.xml`,
`/tmp/zilch-20260911-authorization-controller-regression.log`,
`/tmp/zilch-20260911-authorization-controller-regression.xml`,
`/tmp/zilch-20260911-card-controller-regression.log`, and
`/tmp/zilch-20260911-card-controller-regression.xml`.

**Mechanism (proven):** `Money.of` scales the valid two-decimal GBP `BigDecimal` and calls
`longValueExact()` (`src/main/java/pl/fairydeck/authorization/domain/money/Money.java:24`). The value is
one minor unit above `Long.MAX_VALUE`, so JDK `BigDecimal.longValueExact()` throws
`ArithmeticException: Overflow`. `ApiExceptionHandler` handles `IllegalArgumentException`, but has no
mapping for that arithmetic exception. It therefore cannot produce the RFC 9457 400 response, and Spring
propagates the request failure.

**Why prior tests missed it:** money tests cover currency exponents and excess fractional digits but no long
range boundaries. Controller tests cover bean validation and scale errors, which take the existing
`IllegalArgumentException` handler, but no overflow constructed after deserialization.

## Classification and narrowing ledger

| # | Variable changed | Prediction | Result | Ruled out |
| --- | --- | --- | --- | --- |
| 1 | F1 response `70.9` versus integral score | A fractional success response is mishandled at the risk boundary | `70.9` becomes `Scored(70)`; the fractional regression fails repeatedly | network outage, timeout, and risk-policy decision as the trigger |
| 2 | F1 `70.0` control | The same integer-valued score remains accepted | `Scored(70)` | broad failure of the risk client |
| 3 | F2 amount `...58.08` versus maximum `...58.07` | One minor unit over `long` range changes failure mode | overflow causes `ArithmeticException`; max is accepted | currency scale validation and JSON number parsing as causes |
| 4 | F2 domain versus each web controller | If unchecked arithmetic is the cause, both request mappings fail before their mocked use cases | both web slices fail with `ServletException` caused by `ArithmeticException: Overflow` | use-case, persistence, risk engine, and authorization-policy involvement |

## Verdicts

- Reproduction: **complete**. F1 reproduces at the real outbound HTTP adapter boundary with dynamic WireMock.
  F2 reproduces at the domain boundary and at both inbound HTTP controller slices.
- Classification: **complete**. Both are deterministic product defects on the pinned baseline, not harness,
  external-service, or tooling failures.
- Narrowing: **complete**. The minimal distinguishing properties are a non-integral score and a scaled GBP
  amount one minor unit above `Long.MAX_VALUE`.
- Root cause: **complete**. The code paths and exception/conversion mechanisms are observed in raw failure
  evidence.
- Regression proof: **complete**. Four focused test files are intentionally red on the unfixed revision; the
  corresponding nearest valid controls pass.
- Full real-surface Cucumber proof: **partial**. It was deliberately not added or run because the focused
  HTTP component and web-slice reproductions already isolate the responsible boundaries without starting
  Testcontainers. Remaining proof for the delivery owner is one Cucumber HTTP scenario for fractional risk
  input and error-response assertions for each oversized endpoint on the full Docker-backed stack.

## Handoff

Regression tests retained in the diagnostic worktree:

- `src/test/java/pl/fairydeck/authorization/adapter/out/risk/HttpRiskScorerTest.java`
- `src/test/java/pl/fairydeck/authorization/domain/money/MoneyTest.java`
- `src/test/java/pl/fairydeck/authorization/adapter/in/rest/AuthorizationControllerTest.java`
- `src/test/java/pl/fairydeck/authorization/adapter/in/rest/CardControllerTest.java`

Recommended route: `bug` profile. The repair should make the risk response contract reject fractional
scores fail-closed, normalize `Money` conversion overflow to invalid input at its source, and prove the
existing RFC 9457 mapping is used. No production code, global exception handler, commit, shared service, or
Docker resource was changed by this diagnostic run.
