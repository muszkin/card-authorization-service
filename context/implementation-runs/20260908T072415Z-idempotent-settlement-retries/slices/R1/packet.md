# Repair packet R1 — acceptance harness warm-up

Parent: feature head `3ddbb487e14a749b538a3536207c492f96ac9f65`. Branch `feat/idempotent-settlement-retries-r1`, worktree `/home/muszkin/work/zilch/worktrees/r1`. Profile standard (repair of a final-gate failure). Owner: repair-worker-R1 (model `sonnet`).

## Failure being repaired
See `../../integration/final-gate-failure-001.md`: the first scenario of `card_authorization.feature` is declined because the first risk call on a cold JVM exceeds the 150 ms read timeout. Harness flakiness, not a product defect.

## Required outcome
The acceptance suite must not depend on cold-start latency. Perform a one-time warm-up before the first scenario runs: through the same public API the steps already use, issue a throwaway card and make one low-risk purchase against it (with the low-risk WireMock stub in place), then reset the risk engine so scenarios start from a clean stub set. After the fix, `./gradlew acceptanceTest --rerun-tasks` must pass on two consecutive runs (record both), and the first *scenario* request must no longer be the first request the application ever serves.

## Constraints
- Owned files: `src/acceptanceTest/java/pl/fairydeck/authorization/acceptance/CardAuthorizationSteps.java` and/or `src/acceptanceTest/java/pl/fairydeck/authorization/acceptance/AcceptanceContext.java` only.
- Do NOT change timeouts, `application.yaml`, product code, the feature file, or anything else. Do not add dependencies.
- Keep the step style; no comments except one short sentence where it adds meaning. Compiler runs with `-Xlint:all,-serial -Werror`.
- One commit only, message exactly: `test: warm up the acceptance harness before the first scenario` with a 2–3 line body explaining the cold-start decline. No trailers.
- Stage only your owned files (`git add <path>`), never `-A`.

## Gates you run (working directory `/home/muszkin/work/zilch/worktrees/r1`)
1. Baseline identity guard (toplevel, branch, HEAD = `3ddbb487e14a749b538a3536207c492f96ac9f65`, clean status).
2. `./gradlew compileAcceptanceTestJava` after the change.
3. `./gradlew acceptanceTest --rerun-tasks` twice; save the tail of each run and the per-scenario results (`build/test-results/acceptanceTest/*.xml`) to `/home/muszkin/work/zilch/card-authorization-service/context/implementation-runs/20260908T072415Z-idempotent-settlement-retries/slices/R1/evidence/` (e2e-run-1.txt, e2e-run-2.txt, acceptance-results-1.xml, acceptance-results-2.xml). Also grep the XML for the line `POST /v1/authorizations responded` and record the first few latencies, to show the warm-up request absorbed the cold start.
4. Static: `./gradlew compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava compileAcceptanceTestJava` and `./gradlew test --tests 'pl.fairydeck.authorization.architecture.ArchitectureTest'`.
5. Final identity guard; `git status --porcelain` empty; `git log --oneline -2`.

## Report
Write `/home/muszkin/work/zilch/card-authorization-service/context/implementation-runs/20260908T072415Z-idempotent-settlement-retries/slices/R1/worker-report.md` (only file you may write outside your worktree, plus the evidence dir): identity guards, diff summary, commit SHA, commands with status and evidence paths, the latency evidence, risks. Return the same content.
