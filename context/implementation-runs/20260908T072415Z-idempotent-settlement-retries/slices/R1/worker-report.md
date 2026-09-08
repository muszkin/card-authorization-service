# Worker report — R1 (acceptance harness warm-up)

Worker: repair-worker-R1. Worktree `/home/muszkin/work/zilch/worktrees/r1`, branch
`feat/idempotent-settlement-retries-r1`.

## Identity guards

**Before starting** (matches packet expectation):
```
toplevel: /home/muszkin/work/zilch/worktrees/r1
branch:   feat/idempotent-settlement-retries-r1
HEAD:     3ddbb487e14a749b538a3536207c492f96ac9f65
status:   (empty — clean)
```

**Before commit** (only owned file dirty):
```
toplevel: /home/muszkin/work/zilch/worktrees/r1
branch:   feat/idempotent-settlement-retries-r1
HEAD:     3ddbb487e14a749b538a3536207c492f96ac9f65
status:    M src/acceptanceTest/java/pl/fairydeck/authorization/acceptance/CardAuthorizationSteps.java
```

**Final** (after commit):
```
toplevel: /home/muszkin/work/zilch/worktrees/r1
branch:   feat/idempotent-settlement-retries-r1
HEAD:     80528ab665a5ce44294b69bd90adb60a0051be58
status:   (empty — clean)
log:      80528ab test: warm up the acceptance harness before the first scenario
          3ddbb48 docs: refresh the context map after the reconciliation
```

## Diff summary

One file changed, `CardAuthorizationSteps.java` only (`AcceptanceContext.java` needed no change):

- Added a `private static boolean warmedUp;` field. Static because Cucumber creates a new `CardAuthorizationSteps`
  bean per scenario, so the flag must survive across scenario-scoped instances within the same JVM/suite run.
- `forgetPreviousRiskEngineBehaviour()` (the existing `@Before` hook) now runs `warmUp()` once, the first time it
  fires, before its existing `RISK_ENGINE.resetAll()` call (unchanged, still runs on every scenario).
- New private `warmUp()`: stubs low risk (`theRiskEngineScoresEveryPurchaseAsLowRisk()`), issues a throwaway card
  (`aCardWithACreditLimitOf(100.00, GBP)`) and makes one purchase against it
  (`aPurchaseIsMadeAt(1.00, GBP, DEFAULT_MERCHANT)`) through the same public API the real steps use, then clears
  the scenario state it touched (`answers.clear()`, `cardId = null`, `lastPurchase = null`,
  `lastDecisionTime = Duration.ZERO`). The risk engine reset that follows in the `@Before` body clears the stub
  registered by warm-up, so every scenario — including the first — still starts from an empty stub set.
- One short javadoc sentence on `warmUp()`, no other comments added, matching the file's existing style.

No product code, `application.yaml`, timeouts, feature file, or dependencies were touched.

## Commit

`80528ab665a5ce44294b69bd90adb60a0051be58` — message:
```
test: warm up the acceptance harness before the first scenario

The first outbound risk call runs on a cold JVM and exceeded the
150 ms read timeout, so fail-closed declined the first scenario's
purchase. A one-time throwaway card and purchase now absorb that cost.
```
No trailers, staged with `git add <path>` (single file only).

## Commands run, status, evidence

| # | Command | Status | Evidence |
|---|---|---|---|
| 1 | Identity guard (start) | OK | inline above |
| 2 | `./gradlew compileAcceptanceTestJava` | BUILD SUCCESSFUL (`-Xlint:all,-serial -Werror`, no warnings) | — |
| 3 | `./gradlew acceptanceTest --rerun-tasks` (run 1) | BUILD SUCCESSFUL, 10 tests / 0 skipped / 0 failures / 0 errors | `evidence/e2e-run-1.txt`, `evidence/acceptance-results-1.xml` |
| 4 | `./gradlew acceptanceTest --rerun-tasks` (run 2) | BUILD SUCCESSFUL, 10 tests / 0 skipped / 0 failures / 0 errors | `evidence/e2e-run-2.txt`, `evidence/acceptance-results-2.xml` |
| 5 | `./gradlew compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava compileAcceptanceTestJava` | BUILD SUCCESSFUL | `evidence/static.txt` |
| 6 | `./gradlew test --tests 'pl.fairydeck.authorization.architecture.ArchitectureTest'` | BUILD SUCCESSFUL | `evidence/static.txt` |
| 7 | Identity guard (final) + `git log --oneline -2` | OK, tree clean | inline above |

All evidence files are under
`/home/muszkin/work/zilch/card-authorization-service/context/implementation-runs/20260908T072415Z-idempotent-settlement-retries/slices/R1/evidence/`.

## Latency evidence (the point of the fix)

`grep -o 'POST /v1/authorizations responded [0-9]* in [0-9]* ms' <xml> | head -6` on each run's results file:

Run 1 (`acceptance-results-1.xml`):
```
POST /v1/authorizations responded 201 in 385 ms   <- warm-up purchase (cold JVM, absorbs the cost)
POST /v1/authorizations responded 201 in 9 ms     <- first real scenario's purchase
POST /v1/authorizations responded 201 in 8 ms
POST /v1/authorizations responded 201 in 10 ms
POST /v1/authorizations responded 201 in 9 ms
POST /v1/authorizations responded 201 in 3 ms
```

Run 2 (`acceptance-results-2.xml`):
```
POST /v1/authorizations responded 201 in 387 ms   <- warm-up purchase (cold JVM, absorbs the cost)
POST /v1/authorizations responded 201 in 13 ms    <- first real scenario's purchase
POST /v1/authorizations responded 201 in 10 ms
POST /v1/authorizations responded 201 in 13 ms
POST /v1/authorizations responded 201 in 10 ms
POST /v1/authorizations responded 201 in 3 ms
```

Both runs show the same pattern the failure record diagnosed (first request ~1000 ms, later ones single digits
to tens of ms), except the slow request is now the throwaway warm-up call, comfortably inside its own timeout
budget since it isn't asserted on, and the first real scenario ("A purchase within the available balance is
approved") gets a fast risk response well under the 150 ms timeout. No product code or timeouts changed.

## Risks

- The warm-up purchase's own risk-scoring call could, in principle, still exceed 150 ms in a much slower CI
  environment and get logged as "Risk scoring unavailable" for the throwaway card; that's harmless (the card and
  its outcome are discarded and never asserted on) but would show up as noise in application logs.
- `warmedUp` is a static field scoped to the JVM process running the `acceptanceTest` task; if scenarios were ever
  split across separate JVM forks (they currently run as a single JUnit Platform Suite / one JVM), each fork would
  pay its own one-time warm-up cost — consistent with the intent, not a regression.
- The throwaway card and its one purchase persist in Postgres for the life of the test run (nothing deletes them);
  this matches how every other scenario's fixtures already behave and does not affect any assertion, since all
  scenarios query state scoped to their own `cardId`.
