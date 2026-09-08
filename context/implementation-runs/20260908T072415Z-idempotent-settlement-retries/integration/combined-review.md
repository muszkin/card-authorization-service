# Independent combined review — reviewer-final

**Reviewer:** reviewer-final (independent, read-only; did not implement any candidate in this run)
**Review checkout:** `/home/muszkin/work/zilch/worktrees/review-r1`, detached, verified read-only throughout
**Rubric:** `.claude/skills/implementation-orchestrator/references/review-rubric.md`
**Plan:** `context/plans/2026-09-08-idempotent-settlement-retries.md`

```
cd /home/muszkin/work/zilch/worktrees/review-r1
git rev-parse HEAD          -> 80528ab665a5ce44294b69bd90adb60a0051be58
git status --porcelain      -> (empty)
```

Base checkout verified clean and at the expected candidate SHA before either task began. No files were edited,
staged, or committed by this reviewer at any point; every command below is read-only (`git`, `grep`, `find`,
`cat`, `./gradlew test|compile*Java`, never `./gradlew acceptanceTest`).

---

## Repair candidate 3ddbb48..80528ab

### Inspected SHAs and commands

- Base (parent of repair): `3ddbb487e14a749b538a3536207c492f96ac9f65`
- Head (candidate): `80528ab665a5ce44294b69bd90adb60a0051be58`
- Single commit: `80528ab test: warm up the acceptance harness before the first scenario`

```
git diff 3ddbb487e14a749b538a3536207c492f96ac9f65..80528ab665a5ce44294b69bd90adb60a0051be58   -> 1 file, +17/-0
git diff-tree --no-commit-id --name-only -r 80528ab                                            -> CardAuthorizationSteps.java only
git log -1 --format='%B' 80528ab                                                               -> exact message + 3-line body, no trailers
./gradlew compileAcceptanceTestJava            -> BUILD SUCCESSFUL (clean rebuild not required; re-verified independently)
./gradlew clean compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava compileAcceptanceTestJava
                                                -> BUILD SUCCESSFUL (-Xlint:all,-serial -Werror, all source sets, independently reproduced)
grep -n "forkEvery|parallelForks|maxParallelForks|parallel" build.gradle.kts                    -> no matches (no parallel test config anywhere)
find . -iname "junit-platform.properties" / grep "execution.parallel" in src/acceptanceTest      -> none found
Read AcceptanceScenarios.java, AcceptanceContext.java, CardAuthorizationSteps.java (full files)
Read src/main/java/pl/fairydeck/authorization/adapter/in/rest/AuthorizationController.java (confirms POST /v1/authorizations returns 201 for both approve and decline)
Read context/implementation-runs/.../slices/R1/{packet.md,worker-report.md,gates.md,evidence/*} as claims-to-check
grep -o 'POST /v1/authorizations responded ... in ... ms' on both acceptance-results-{1,2}.xml -> reproduced the claimed latency series
grep -c '<testcase' / 'failures=\"0\" errors=\"0\"' on both acceptance-results-{1,2}.xml        -> 10 tests, 0 failures, 0 errors, both runs
```

### Logical trace

`CardAuthorizationSteps.java:39` adds `private static boolean warmedUp;`. `forgetPreviousRiskEngineBehaviour()`
(`@Before`, line 53) now does, in order: if `!warmedUp`, set it and call `warmUp()`; then unconditionally
`RISK_ENGINE.resetAll()`. `warmUp()` (line 62): registers the low-risk WireMock stub, issues a throwaway card,
makes one purchase through the same public API the real steps use, then resets every field it touched
(`answers.clear()`, `cardId = null`, `lastPurchase = null`, `lastDecisionTime = Duration.ZERO`) back to the
values they have on a freshly constructed instance.

**Runs exactly once per JVM.** Cucumber is wired as a single JUnit Platform Suite
(`AcceptanceScenarios.java`: `@Suite` + `@IncludeEngines("cucumber")`), and `build.gradle.kts` sets no
`maxParallelForks`, `forkEvery`, or any `junit.jupiter.execution.parallel.*` / `cucumber.execution.parallel.*`
property anywhere in the repository (confirmed by grep). Gradle's `Test` task default (`forkEvery = 0`, one
worker JVM) applies, so the whole suite runs sequentially in one JVM. `CardAuthorizationSteps` is scenario-scoped
(a new Spring/Cucumber bean per scenario), but `warmedUp` is `static`, so it is genuinely a once-per-JVM flag,
independent of scenario order (no `cucumber.execution.order` property is set anywhere, so even if order changed,
the mechanism is order-independent by construction — whichever scenario runs first pays the cost once).

**Leaves every scenario, including the first, with a clean stub set and clean step state.** The stub `warmUp()`
registers is cleared by the very next line, `RISK_ENGINE.resetAll()` (WireMock's `resetAll()` clears stub
mappings, the request journal and scenario state), before the real scenario's own `Background` step runs. The
four fields `warmUp()` touches are reset to their construction-time values afterward. No `@Then` step in the file
asserts on unscoped/global state (all balance/transaction assertions are scoped by `cardId`; verified by reading
the full file), so the throwaway card's row persisting in Postgres for the run's lifetime (acknowledged in the
worker's own risk note) cannot leak into any assertion.

**Cannot fail a scenario even if declined on a slow machine.** `AuthorizationController.authorize` (verified by
reading the controller) returns `ResponseEntity.created(...)` — HTTP `201` — for both `APPROVED` and `DECLINED`
outcomes; the decision is carried in the response body, not the status code. `warmUp()`'s call chain
(`aPurchaseIsMadeAt` -> `purchase` -> `send` -> `.retrieve().body(...)`) throws only on a non-2xx HTTP status, and
nothing in `warmUp()` inspects `answers` before clearing it. A slow-machine decline of the throwaway purchase is
therefore silently absorbed exactly as the worker's own risk note claims — independently confirmed, not merely
trusted.

**Does not change timeouts or product behaviour.** The diff touches only
`src/acceptanceTest/java/.../CardAuthorizationSteps.java`; no `src/main`, no `application.yaml`, no
`build.gradle.kts`, no feature file line was touched (`git diff --stat` shows one file, `+17/-0`).

**Style / `-Werror` / commit convention.** One short javadoc sentence, no other comments, matching file style.
Clean rebuild of every source set under `-Xlint:all,-serial -Werror` reproduced independently (see commands
above). Commit message is exactly the mandated string with a 3-line body and no trailers; single commit; only
the owned file is staged (confirmed via `git diff-tree --name-only`).

### Findings

**RC-1 — LOW — `CardAuthorizationSteps.java:39,54-58` — latent non-thread-safety of the warm-up guard**
- Violated criterion: none currently (rubric #7, concurrency and reliability) — no acceptance criterion requires
  thread safety here, and no parallel execution is configured today.
- Failure scenario: `if (!warmedUp) { warmedUp = true; warmUp(); }` is a non-atomic check-then-act on a plain
  (non-`volatile`, non-`AtomicBoolean`) field. If a future change ever enables JUnit5/Cucumber parallel scenario
  execution (nothing today does), two scenario threads could race into `warmUp()` concurrently, each creating a
  throwaway card/purchase and each seeing `!warmedUp` true, defeating the "exactly once" property and doubling
  warm-up cost (harmless) or interleaving stub registration/reset in a way that could leak an unstubbed request
  to a real scenario (harmful).
- Evidence: source above; no parallel config anywhere in `build.gradle.kts` or `src/acceptanceTest`, confirmed by
  grep, so the race is unreachable in the current codebase.
- Required outcome: none required today; if parallel scenario execution is ever introduced, this guard must be
  revisited (e.g., an idempotent one-time initializer or a lock) at that time.
- Blocks: no.

No CRITICAL, HIGH, or MEDIUM findings against this candidate.

### Dimensions inspected

1. Acceptance correctness — the packet's required outcome ("the first scenario request must no longer be the
   first request the application ever serves") is met and independently confirmed via the latency grep.
2. Regression / preserved behavior — zero product, config, or feature-file changes; clean compiles across every
   source set reproduced independently.
3. Logical correctness — guard placement (before the unconditional `resetAll()`) and field-reset list are both
   correct and complete against the class's own field list.
4. Edge cases — slow-machine decline of the warm-up purchase (no assertion, cannot fail); scenario-order
   independence (mechanism keys off "first execution," not "first declared scenario").
5. Security and privacy — not applicable; test-only harness code, no new trust boundary.
6. Data and migration safety — throwaway card/purchase persist in Postgres for the run's life; harmless, since
   every assertion in the file is `cardId`-scoped (verified by reading the whole file).
7. Concurrency and reliability — see RC-1; no reachable race today given confirmed single-JVM, no-parallel
   execution.
8. Architecture / ADR compliance — test-only change, no layering or ADR implication.
9. Operability — none needed or removed; latency evidence explicitly captured per the packet's own ask.
10. Tests and E2E strength — two independent full `acceptanceTest --rerun-tasks` runs, both 10/10, 0
    failures/errors, reproduced by inspecting the XML directly rather than trusting the worker's summary table.
11. Scope and maintainability — one file, one commit, exactly the owned surface, no unrelated changes, no dead
    code, no TODOs, comment budget respected.

### Verdict: PASS

Rubric rule applied: "`PASS` — no unresolved material findings for the inspected SHA." The single finding (RC-1)
is LOW and reveals no required acceptance or repository-policy violation (no parallel-execution policy exists to
violate); every dimension was independently verified against the source and the evidence files, not merely
trusted.

---

## Combined feature 37e3d11..80528ab

### Inspected SHAs and commands

- Base (approved plan's base / integration target before the feature): `37e3d118c1116bb2e28ae754a26156f7c44100eb`
- Head: `80528ab665a5ce44294b69bd90adb60a0051be58`
- Five commits: `f0f62f4` (test) -> `ddd71fa` (feat) -> `b0e9abc` (docs) -> `3ddbb48` (docs) -> `80528ab` (test/repair)

```
git log --format='%h %s' 37e3d118c1116bb2e28ae754a26156f7c44100eb..80528ab665a5ce44294b69bd90adb60a0051be58
git diff --stat 37e3d11..80528ab                              -> 32 files, +1497/-29
git diff 37e3d11..80528ab -- src/main/.../AuthorizationLifecycle.java   (read in full, both diff and final file)
git diff 37e3d11..80528ab -- src/main/.../domain/                       -> empty (domain untouched, confirmed)
git diff 37e3d11..80528ab -- src/test/.../AuthorizationLifecycleTest.java (read in full)
git diff 37e3d11..80528ab -- DECISIONS.md README.md            (read in full, cross-checked against code)
git diff 37e3d11..80528ab -- src/acceptanceTest/...             (feature file + steps, cross-checked against plan D6/S1 scope)
Read AuthorizationStatus.java, Authorization.java (capture/reverse/expire/requireStatus), AuthorizationController.java
Read application/.agents/project-context.md diff and full file (invariant text cross-checked against code)
./gradlew test --tests 'pl.fairydeck.authorization.application.AuthorizationLifecycleTest'   -> BUILD SUCCESSFUL
./gradlew compileAcceptanceTestJava                                                            -> BUILD SUCCESSFUL
./gradlew test --tests 'pl.fairydeck.authorization.architecture.ArchitectureTest'              -> BUILD SUCCESSFUL
./gradlew test (full unit suite)                                                                -> BUILD SUCCESSFUL
./gradlew clean compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava compileAcceptanceTestJava
                                                                                                 -> BUILD SUCCESSFUL (-Werror, clean)
git ls-tree -r 80528ab -- context/implementation-runs/                                          -> lists RUN.md/run.json/events.jsonl/slices/S1/*; does NOT list integration/final-gate-failure-001.md or slices/R1/*
git status --porcelain in /home/muszkin/work/zilch/card-authorization-service (main checkout)   -> "?? context/implementation-runs/", "?? context/plans/" (untracked)
grep for secrets/credentials/absolute-path leakage across the whole docs diff (password|secret|token|BEGIN|jdbc:...|redis://) -> only localhost Testcontainers JDBC URLs and expected repo-local worktree paths, no credentials
```

### Logical trace of the product change (ddd71fa on top of f0f62f4)

`AuthorizationLifecycle.settle` gained a third parameter, `Set<AuthorizationStatus> alreadySatisfiedBy`, checked
immediately after the second (post-lock) `load(authorizationId)` and before any ledger append, save, or outbox
record:

```java
private Authorization settle(UUID authorizationId,
        BiFunction<Authorization, Instant, List<LedgerEntry>> transition, Set<AuthorizationStatus> alreadySatisfiedBy) {
    ledger.lock(load(authorizationId).cardId());
    Authorization authorization = load(authorizationId);
    if (alreadySatisfiedBy.contains(authorization.status())) {
        return authorization;
    }
    ...
}
```

`capture` passes `CAPTURE_ALREADY_SATISFIED = {CAPTURED}`; `reverse` passes
`REVERSE_ALREADY_SATISFIED = {REVERSED, EXPIRED}`; `releaseExpiredHolds` passes `NEVER_ALREADY_SATISFIED = {}`
(bit-for-bit unchanged expiry-sweep behavior). `AuthorizationStatus` has exactly five values
(`APPROVED, DECLINED, CAPTURED, REVERSED, EXPIRED`); both sets correctly exclude `APPROVED`/`DECLINED` and the
opposite terminal status, so a genuinely conflicting transition still falls through into the untouched domain
method (`Authorization.capture`/`reverse`, confirmed byte-identical to `37e3d11`), which still throws
`IllegalStateException` -> `409` for every other state. Four new `AuthorizationLifecycleTest` cases
(capture-on-CAPTURED, reverse-on-REVERSED, reverse-on-EXPIRED, capture-after-reverse-still-throws) and one new
Cucumber scenario (`Retrying a capture after it succeeded changes nothing`) were added and pass; the red evidence
(`slices/S1/evidence/red-unit.txt`) shows all three replay tests failing with the exact pre-fix
`IllegalStateException` messages before `ddd71fa`, confirming test-first was real, not staged after the fact.

### Cross-commit documentation consistency

`DECISIONS.md §17` and the two `README.md` table rows were read in full and checked line-by-line against the
`EnumSet` contents: capture replays only on `CAPTURED` (README/DECISIONS both say capture-after-reversal-or-expiry
stays `409`); reverse replays on `REVERSED` **and** `EXPIRED` (README/DECISIONS both say so explicitly). This
matches the code exactly, including the specific asymmetry the task asked to verify (reverse replays on `EXPIRED`,
capture does not). The `application/.agents/project-context.md` invariant paragraph was likewise checked against
the code and is accurate.

### Findings

**CF-1 — MEDIUM — `context/implementation-runs/20260908T072415Z-idempotent-settlement-retries/slices/S1/gates.md:11-12`, `run.json:4,58-59`, `RUN.md:4,13` — committed run-ledger self-contradicts within the same commit and is stale at HEAD**
- Violated criterion: combined-review emphasis "anything in the docs/ledger that is inaccurate" (task instruction,
  mirrors rubric dimension 11, scope and maintainability / accidental generated output).
- Failure scenario: a future engineer or auditor reading the committed run ledger at `80528ab` to understand
  whether S1 was actually reviewed and E2E-tested would be told **no** by `gates.md` ("independent review |
  NOT_RUN", "real-surface E2E ... | NOT_RUN") and by `run.json` ("review": "NOT_RUN", "e2e": "NOT_RUN",
  top-level `"state": "SLICES_RUNNING"`) — while, in the very same commit `b0e9abc`, `slices/S1/review.md` records
  an explicit **PASS** verdict from reviewer-S1, `slices/S1/evidence/e2e.txt` and `acceptance-results.xml` record
  a green 10/10 acceptance run, and `events.jsonl` (also in `b0e9abc`) logs `REVIEW_GREEN` (07:37:36Z),
  `E2E_GREEN` (07:37:46Z), `READY_FOR_INTEGRATION`, `INTEGRATED`, and `FEATURE_HEAD_GREEN` (07:38:20Z) for the
  identical SHA `ddd71fa`. `RUN.md` additionally says "Next action: dispatch worker-S1 with `slices/S1/packet.md`"
  at HEAD, even though S1 finished, was integrated, and a subsequent final-gate failure and repair (R1) already
  happened by the time `80528ab` was committed.
- Evidence: exact file/line contents quoted above, cross-referenced against `events.jsonl` lines 15-19 and
  `review.md` lines 113-117, all read directly from the candidate checkout.
- Required outcome: reconcile `gates.md`, `run.json`, `state.json`, and `RUN.md` to the true final state (review
  PASS, E2E PASS, S1 integrated, final-gate failure found and repaired via R1) before this branch is delivered —
  either by updating them in place or by explicitly superseding them with a dated addendum, consistent with how
  this repository's own methodology treats the run ledger as the audit trail for a payments-service change.
  Does **not** require any change to production code.
- Blocks: yes (material — the delivered branch's own audit trail contradicts itself about whether required gates
  ran, which is exactly the class of ledger inaccuracy the combined review was asked to catch).

**CF-2 — MEDIUM — `integration/final-gate-failure-001.md`, `slices/R1/*` (not present in `git ls-tree 80528ab`) — the repair's own justification and evidence are not committed to the branch**
- Violated criterion: same combined-review emphasis as CF-1, plus rubric #9 (operability/diagnosability — the
  reason a fix exists should be discoverable from the repository, not only from a local machine's scratch files).
- Failure scenario: `git ls-tree -r 80528ab -- context/implementation-runs/` lists every S1 ledger file but
  **none** of `integration/final-gate-failure-001.md` or `slices/R1/{packet.md,worker-report.md,gates.md,
  evidence/*}` — the exact files this reviewer was pointed to as evidence for the repair candidate. Independently,
  `git status --porcelain` in the primary checkout `/home/muszkin/work/zilch/card-authorization-service` (on
  `main` at `37e3d11`) shows `context/implementation-runs/` and `context/plans/` as **untracked** — this run's
  entire plan, ledger, and evidence trail exists only as loose files on one machine, not in any commit reachable
  from `80528ab`. If `feat/idempotent-settlement-retries-r1` is rebase-merged as the plan specifies, `main` will
  gain the one-line-body commit "`test: warm up the acceptance harness before the first scenario`" with no
  committed trace of the `final-gate-failure-001.md` diagnosis (cold-JVM risk-call timeout, latency series) that
  justified it, unlike S1's evidence, which the docs commit `b0e9abc` did commit.
- Evidence: `git ls-tree -r 80528ab -- context/implementation-runs/` output above; `git status --porcelain`
  output above.
- Required outcome: before delivery, commit the final-gate-failure record and the R1 packet/worker-report/gates/
  evidence to the branch (mirroring how S1's evidence was committed), or fold their substance into a final
  reconciliation docs commit, so the repaired branch is self-explanatory from `git log`/`git show` alone. Does
  **not** require any change to production code.
- Blocks: yes, for the same traceability reason as CF-1.

**CF-3 — LOW, non-blocking — missing HTTP-boundary coverage for reverse-side replay paths**
- Violated criterion: rubric #10 (tests and E2E strength) / combined-review emphasis "missing end-to-end paths."
- Failure scenario: the acceptance suite (real HTTP boundary, `card_authorization.feature`) proves only the
  capture-retry idempotency path ("Retrying a capture after it succeeded changes nothing"). Reverse-retry
  (`REVERSED` -> `REVERSED`) and reverse-after-`EXPIRED` replay are proven only at
  `AuthorizationLifecycleTest` (in-memory fakes), never through `AuthorizationController.reverse`, its JSON
  response shape, or `ApiExceptionHandler` wiring. A regression isolated to the `reverse` controller method or its
  serialization (as opposed to `capture`'s, which is proven) would not be caught by `./gradlew acceptanceTest`.
- Evidence: `card_authorization.feature` (full file read) contains exactly one retry scenario, capture-only;
  `AuthorizationController.capture`/`reverse` are structurally identical one-line delegations, so residual risk
  is low but not zero.
- Required outcome: none required to unblock — this is an explicit, plan-approved scope decision (plan's S1
  "Behavior and acceptance": "Same for reverse on REVERSED (unit level)." and the Coverage matrix row "retried
  reverse replays, books nothing | S1 | ... | unit"), not a deviation from the approved plan. Flagging per the
  combined-review's explicit instruction to surface missing E2E paths; an optional future acceptance scenario for
  the reverse side would close the residual gap.
- Blocks: no (deliberate, approved-in-plan scope; low residual risk given the identical controller pattern).

No CRITICAL or HIGH findings. No deviation was found between the final code and the approved plan on any
in-scope behavior (D1-D7 all implemented as decided, including the operator override D5).

### Dimensions inspected

1. Acceptance correctness — every plan Coverage-matrix row (capture replay, reverse replay, reverse-on-expired,
   conflicting-transitions-still-409, racing-retry-under-lock, no-second-event) is implemented and either
   unit- or E2E-proven, matching the plan exactly (including D5, the operator override).
2. Regression / preserved behavior — `Authorization.java` (domain) byte-identical to `37e3d11`; full unit suite
   (`./gradlew test`) and `ArchitectureTest` reproduced green independently at `80528ab`; controller-level 409
   mapping test (`AuthorizationControllerTest.answersConflictWhenTheTransitionIsNotAllowed`) unaffected (mocks
   the use case, doesn't exercise the new branch).
3. Logical correctness — `EnumSet` membership checked against the 5-value `AuthorizationStatus` enum by hand;
   correct and minimal for both operations; check sits after lock + reload as D3 requires.
4. Edge cases — capture-on-CAPTURED, reverse-on-REVERSED, reverse-on-EXPIRED, capture-after-reverse (still
   throws) all unit-tested and independently re-run green; capture-on-EXPIRED and anything-on-DECLINED covered
   by set non-membership plus pre-existing domain/controller tests.
5. Security and privacy — no new trust boundary; DECISIONS §15 accepted risk (no caller identity) unchanged and
   still accurately documented; no secrets in any changed or added file (grepped the full docs diff).
6. Data and migration safety — no schema/migration files touched; replay path performs zero writes, confirmed by
   code reading and by ledger/outbox-size assertions in the new unit tests.
7. Concurrency and reliability — pre-existing `ledger.lock` + second-load pattern unchanged; new check sits
   after both, so a racing retry is still decided from a lock-serialized, authoritative read; no new round trip.
8. Architecture / ADR compliance — domain untouched and framework-free; `application` still does not import
   `adapter` (unchanged); no new Spring beans; consistent with DECISIONS §4, §8, §13, §14, and the new §17.
9. Operability — see CF-2 (diagnosability of the repair itself is impaired by the missing commit); the product
   change adds no new logs/metrics, none needed.
10. Tests and E2E strength — deliberate-break reasoning re-verified for the new unit tests (removing the
    short-circuit routes into the domain's `IllegalStateException`, an unhandled exception in the test body, so
    tests would error rather than silently pass); see CF-3 for the one real E2E coverage gap, which is
    plan-approved.
11. Scope and maintainability — diff touches exactly the files the plan/S1 predicted plus the expected docs/run
    ledger and the R1 repair; no unrelated production-code changes, no dead code, no stubs, no hidden TODOs; see
    CF-1/CF-2 for the ledger-accuracy exception. Absolute local paths under `context/implementation-runs/` and
    `context/plans/` are the repository's own documented convention for run ledgers (per the task's own
    instruction) and contain no credentials, tokens, or external endpoints — only localhost Testcontainers URLs
    and this machine's own worktree paths — so they are not flagged as a leak.

### Verdict: FAIL

Rubric rule applied: "`FAIL` — one or more material findings." CF-1 and CF-2 are MEDIUM and material to the
combined review's explicit mandate to check ledger accuracy: the committed run ledger self-contradicts within a
single commit about whether required gates (independent review, E2E) ran, and the repair candidate's own failure
diagnosis and evidence are absent from the branch's git history. Both are documentation/process defects with a
mechanical fix (a further docs/reconciliation commit) and **do not** indicate any defect in the shipped product
code: `AuthorizationLifecycle`'s replay logic, its unit tests, the acceptance scenario, DECISIONS §17, the README
rows, and the R1 harness warm-up all independently re-verified correct, accurate, and green on every other rubric
dimension (see Repair candidate verdict, PASS, above). Recommended path: land one more reconciliation docs commit
that (a) corrects `gates.md`/`run.json`/`state.json`/`RUN.md` to the true final state and (b) commits the
`final-gate-failure-001.md` and `slices/R1/*` evidence, then re-review only that commit's effect on the ledger
(CF-1/CF-2) — the product-code surface needs no further review.
