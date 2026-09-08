# Idempotent settlement retries Implementation Plan

**Status:** approved (2026-09-08, operator; D5 overridden as recorded below)
**Date:** 2026-09-08
**Request:** review finding on the recruitment deliverable: a merchant that retries `POST /v1/authorizations/{id}/capture` (or `/reverse`) after the first call already succeeded receives `409 Conflict` instead of the same answer. The brief's fintech checklist asks for idempotent POST operations.
**Plan location:** `context/plans/2026-09-08-idempotent-settlement-retries.md` (tracked; first plan in this repository, establishes `context/plans/` as the planning convention)

## Outcome

A merchant or payment switch that loses the response to a capture or reversal and sends the same request again gets the
same `200` answer with the authorization in its settled state. Nothing is booked twice: no second `HOLD_RELEASE` or
`CAPTURE` entry, no second outbox event. Genuinely conflicting transitions (capture after reversal, reversal after
capture, capture after expiry) keep answering `409`, because there the caller must learn that the money went
somewhere else. A reversal of an already expired hold answers `200`: the hold is released either way, which is
what the caller asked for.

## Scope

### In scope

- `POST /v1/authorizations/{id}/capture` on an authorization already `CAPTURED` returns `200` with the current representation and changes nothing.
- `POST /v1/authorizations/{id}/reverse` on an authorization already `REVERSED` or `EXPIRED` returns `200` with the current representation and changes nothing.
- A retry racing the original request is decided under the existing per-card ledger lock: exactly one transition happens, the other call replays it.
- Unit coverage in `AuthorizationLifecycleTest`, an acceptance scenario over HTTP in `card_authorization.feature`.

### Out of scope

- An `Idempotency-Key` header on settlement endpoints (the authorization id is the natural key; see D2).
- Caller identity and per-object authorization (DECISIONS.md §15, accepted risk).
- Partial captures, refunds, changes to `POST /v1/authorizations` replay semantics (DECISIONS.md §8).
- Any change to the domain transitions in `Authorization` or to the schema.

## Evidence map

| Evidence | What it establishes | Provenance |
| --- | --- | --- |
| `context/map/INDEX.md`, `context/map/manifest.json` (snapshot `b4bef16`, generated 2026-09-07T14:57:44Z, digest valid) | router to application/adapter contexts; stale only for `build.gradle.kts` (`-Xlint:all,-serial -Werror` added in `37e3d11`) and `.claude/skills` (`98b9baf`); planned surfaces unchanged since the snapshot | Observed |
| `src/main/java/pl/fairydeck/authorization/application/AuthorizationLifecycle.java` (`capture`, `reverse`, `settle`, `load`) | every settlement: load, `ledger.lock(cardId)`, re-load, apply domain transition, append entries, save, record event | Observed |
| `src/main/java/pl/fairydeck/authorization/domain/authorization/Authorization.java:77-101` | `capture`/`reverse`/`expire` require `APPROVED` and throw `IllegalStateException` otherwise | Observed |
| `src/main/java/pl/fairydeck/authorization/adapter/in/rest/ApiExceptionHandler.java:23-26` | `IllegalStateException` -> `409` problem detail | Observed |
| `src/main/java/pl/fairydeck/authorization/adapter/in/rest/AuthorizationController.java:41-49` | settlement endpoints return `AuthorizationResponse.from(lifecycle.capture(id))` with `200` | Observed |
| `src/test/java/pl/fairydeck/authorization/application/AuthorizationLifecycleTest.java` | in-memory fakes for ledger, authorizations, outbox; `lifecycleAt(Instant)` helper; existing capture/reverse/sweep specs | Observed |
| `src/test/java/pl/fairydeck/authorization/domain/authorization/AuthorizationTest.java:99` `allowsEachAuthorizationToBeSettledOnlyOnce` | the domain must keep refusing a second transition | Observed |
| `src/acceptanceTest/.../card_authorization.feature`, `CardAuthorizationSteps.java` (`transition`, `answers`, `balance()`) | HTTP scenarios for capture and reversal exist; a plain `RestClient` throws `HttpClientErrorException.Conflict` on `409` | Observed |
| `DECISIONS.md` §8, §13, §15 | idempotency of `POST /v1/authorizations`, expiry rule, caller authentication out of scope | Observed |
| `CLAUDE.md`, `src/main/java/.../application/CLAUDE.md` | test first with an observed red build, `test:` then `feat:` commits, constructor injection, lock before ledger writes, no amend/squash | Observed |
| `.github/workflows/build.yml`; `gh api .../branches/main/protection` -> 404; `gh pr list` -> 0 | CI runs on `pull_request` and push to `main`; no branch protection; history is linear on `main` | Observed |
| `./gradlew build` at `37e3d11` | baseline green: 102 + 20 + 9 tests | Observed |
| Sonar configuration | none in build files or CI | Observed |

## Current state

`AuthorizationController.capture(id)` -> `AuthorizationLifecycle.capture(id)` -> `settle(id, Authorization::capture)`:
load -> `ledger.lock(cardId)` -> re-load under the lock -> `Authorization.capture(now)`. On the second call the
authorization is already `CAPTURED`, so `requireStatus(APPROVED, "capture")` throws `IllegalStateException("Cannot
capture on a CAPTURED authorization")`, which `ApiExceptionHandler` maps to `409`. The lock and the double load already
give the correct place to decide a replay: after the re-load, the current status is authoritative.

Reusable: the `settle` template, the fakes in `src/testFixtures`, the `transition(action)` step and `answers` list in the
acceptance steps, `Balance`-based assertions.

## Decision and assumption ledger

| Item | Provenance | Decision state | Evidence or rationale | Consequence |
| --- | --- | --- | --- | --- |
| D1 A retry is replayed only when the authorization is already in a state that satisfies the request (`CAPTURED` for capture; `REVERSED` or `EXPIRED` for reverse, see D5). Any other non-`APPROVED` state stays a `409`. | Inferred | Decided | idempotency means "same request, same effect"; a different terminal state is a different fact the caller needs | one status comparison in `settle`; domain untouched |
| D2 No `Idempotency-Key` header for settlements. | Inferred | Decided | the authorization id identifies the operation; a header would need a second key store for no gain | no API change |
| D3 The replay check lives in `AuthorizationLifecycle.settle`, after the re-load under the card ledger lock. | Observed (structure) | Decided | the lock serializes a racing original and retry; the re-loaded status is authoritative; domain transitions stay strict (`AuthorizationTest:99`) | no change to `Authorization` |
| D4 A replay appends no ledger entry and records no outbox event. | Inferred | Decided | the event for the transition was already recorded once; consumers dedupe by event id, but there is no reason to send a second one | assertion in unit test and via balance in the acceptance scenario |
| D5 Capture after `EXPIRED` answers `409` (DECISIONS §13); reverse after `EXPIRED` answers `200` with the current representation (status `EXPIRED`). | User-confirmed | Decided (operator override of the planner's proposal, 2026-09-08) | the hold is already released, so the outcome the caller asked for is a fact; the status still tells the truth about how it happened | reverse replays on `REVERSED` and `EXPIRED`; capture replays on `CAPTURED` only |
| D6 Verification surface is the public HTTP API (API-only service, no UI). | Observed (README) | N/A | Cucumber over HTTP is the repository's real-surface harness | consumer-boundary E2E |
| D7 Delivery through a GitHub PR merged with "rebase and merge". | Observed (linear history, CI on `pull_request`) | Decided | keeps the `test:` -> `feat:` pair visible on `main`, gives CI proof on the PR head before merge | first PR in the repository |

## Socratic challenge ledger

| Risk rank | Assumption | Strongest counterexample or failure mode | Evidence | Consequence if false | Decision and accountable owner |
| --- | --- | --- | --- | --- | --- |
| high | A retried capture must not book twice | none: the domain already refuses; the defect is the `409`, not double booking | `AuthorizationTest:99`, `settle` under lock | n/a | confirmed |
| high | A racing retry cannot observe `APPROVED` after the original committed | both calls take the card ledger lock before the authoritative read; READ COMMITTED gives a fresh snapshot per statement | `JdbcLedgerRepository.lock`, DECISIONS §4 | the loser would throw `409` instead of replaying | confirmed; covered by the existing lock, unit test proves the replay path |
| medium | `200` with the current state is the right replay answer | some APIs return `409` plus the current state | brief: "idempotency keys on POST operations"; DECISIONS §8 already replays `POST /v1/authorizations` with the same status | inconsistent API | confirmed (consistency with §8) |
| medium | The replay must not emit a second event | an at-least-once consumer would dedupe anyway | `OutboxRelay` javadoc, event ids | noise on the bus, misleading audit | confirmed (D4) |
| medium | The check belongs in the application layer | a domain method `capture` returning an empty list when already captured would be "smarter" | that hides a no-op inside a transition and still needs the application to skip save/outbox | leaky contract | confirmed (D3) |
| low | No new header is needed | a switch may retry with a different authorization id if it lost the id too | then it never had a successful first call; it must re-authorize | none | confirmed (D2) |
| low | Reverse after expiry should replay as well | money is released in both cases | status semantics differ; DECISIONS §13 | caller gets `409` with the reason | planner proposed `409`; operator overrode to `200` (the released hold is the outcome the caller wanted); owner: repository author |

## Solution contract

### Actors and permissions

Merchant or payment switch calling the settlement endpoints. No caller identity exists in this service (DECISIONS §15,
accepted risk); anyone who knows an authorization id can settle or replay it.

### Happy path

1. The merchant captures an approved authorization: `POST /v1/authorizations/{id}/capture` -> `200`, status `CAPTURED`, ledger gains `HOLD_RELEASE` + `CAPTURE`, one event recorded.
2. The response is lost; the merchant sends the identical request again -> `200`, status `CAPTURED`, same id, ledger and outbox unchanged.
3. The same holds for `reverse` -> `REVERSED`.

### Edge cases and failure behavior

| Case | Required behavior | Recovery or fallback | Slice |
| --- | --- | --- | --- |
| capture retried after `CAPTURED` | `200`, same representation, no ledger entry, no event | none needed | S1 |
| reverse retried after `REVERSED` | `200`, same representation, no ledger entry, no event | none needed | S1 |
| capture after `REVERSED` / reverse after `CAPTURED` | `409` (unchanged) | caller reads the status | S1 (preserved) |
| capture after `EXPIRED` | `409` (unchanged, DECISIONS §13) | caller reads the status | S1 (preserved) |
| reverse after `EXPIRED` | `200`, current representation (status `EXPIRED`), no ledger entry, no event | none needed | S1 |
| capture or reverse of a `DECLINED` authorization | `409` (unchanged) | n/a | S1 (preserved) |
| original and retry racing on the same authorization | one transition, the other call replays under the lock | lock serializes | S1 |
| unknown id | `404` (unchanged) | n/a | preserved |

### Data, state, and contracts

No schema change. No change to `AuthorizationResponse`. `Authorization` transitions unchanged. Public contract change:
settlement endpoints become idempotent for repeated identical requests (documented in README API table and in a new
DECISIONS entry at closeout).

### Threat model

Trigger: payments. Boundaries: public HTTP -> application -> Postgres.

| Asset | Threat | Entry point | Existing control | Gap | Slice that closes it |
| --- | --- | --- | --- | --- | --- |
| cardholder's available balance | double settlement through repeated requests (tampering/DoS on the ledger) | `POST .../capture`, `.../reverse` | strict domain transitions, per-card ledger lock, re-load under the lock | none; this change only replaces the `409` with a replay | S1 (unit test proves one transition, one event) |
| settlement of someone else's authorization | spoofing: any caller may settle any id | same | none (no caller identity) | accepted risk, DECISIONS §15 | accepted risk, owner: repository author |
| audit trail | repudiation: a replay silently altering history | same | append-only ledger (DB trigger), events with ids | none; replay writes nothing | S1 |

- **Trust boundaries crossed:** public HTTP into the application; unchanged.
- **Secrets introduced or moved:** none.
- **Abuse cases promoted to acceptance:** retried capture books nothing (unit + acceptance).

### Non-functional constraints

- No additional database round trip: the replay decision uses the re-load that `settle` already performs.
- Response time for settlements unchanged; the authorization budget (200-300 ms) is not on this path.

### Rollout and rollback

Single deployable; no flag, no migration. Rollback is a revert of the `feat:` commit.

## Technology decisions

| Decision | Version or policy | Requirement driving it | Rationale | Rejected alternatives | Source |
| --- | --- | --- | --- | --- | --- |
| existing stack only | Spring Boot 4.1.1, JUnit 6.0.3, Cucumber 7.34.8 as resolved | none new | change is one comparison and tests | none considered | `build.gradle.kts` |

## Global implementation constraints

- Test first: a failing unit test and a failing acceptance scenario are committed as `test:` and observed red before the `feat:` commit (`CLAUDE.md`).
- Constructor injection only; no new beans needed.
- Domain stays framework-free and its transitions stay strict; `Authorization` is not edited.
- Every balance-changing transaction keeps calling `LedgerRepository.lock(cardId)` before reading the ledger.
- `./gradlew test` must remain runnable without Docker.
- Compiler runs with `-Xlint:all,-serial -Werror`.
- Commit messages: Conventional Commits, English, no trailers about tooling.
- Do not amend or squash; do not force-push.

## Execution topology and gate contract

| Concern | Repository-proven value | Evidence | Execution consequence |
| --- | --- | --- | --- |
| Approved base and integration target | `main` at `37e3d118c1116bb2e28ae754a26156f7c44100eb`; integration target `main` | `git rev-parse`, `git log` | feature branch starts here |
| Feature branch and merge policy | `feat/idempotent-settlement-retries`; linear history; integrate by GitHub PR with **rebase and merge** so the `test:` and `feat:` commits land as they are | DECISIONS §16, linear `git log`, no protection rules | orchestrator opens the PR; merge only inside the authorization envelope |
| Required PR checks | workflow `build` (job `build`) runs on `pull_request`; no branch protection | `.github/workflows/build.yml`, `gh api` 404 | CI must be green on the PR head SHA before merge |
| Staging trigger and revision proof | none | no deployment definition (README, context map) | `NOT_APPLICABLE` |
| Production target | none | same | `NOT_APPLICABLE` |
| Focused tests | `./gradlew test --tests 'pl.fairydeck.authorization.application.AuthorizationLifecycleTest' --tests 'pl.fairydeck.authorization.domain.authorization.AuthorizationTest'` | `build.gradle.kts` suites | repository root; no Docker |
| Static analysis | `./gradlew compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava compileAcceptanceTestJava` (`-Xlint:all,-serial -Werror`) and `./gradlew test --tests 'pl.fairydeck.authorization.architecture.ArchitectureTest'` | `build.gradle.kts` (`37e3d11`) | repository root |
| Dependency, secret and license scanning | none configured | no scanner in build/CI | diff-level secret and introduced-dependency check by the reviewer; the slice adds no dependency |
| Sonar | not configured | no `sonar-project.properties`, nothing in CI | `NOT_APPLICABLE` with this evidence |
| Real-surface E2E | `./gradlew acceptanceTest` (Cucumber over HTTP, Testcontainers + WireMock) | `src/acceptanceTest`, README "Tests" | consumer-boundary E2E; the public API is the product surface, there is no UI |
| Full feature verification | `./gradlew build` | CI workflow | after the slice and again on the assembled feature SHA; CI on the PR head |

### Worktree resource isolation

| Resource | Isolation or serialization rule | Evidence |
| --- | --- | --- |
| application/test ports | random (`RANDOM_PORT`, Testcontainers mapped ports, WireMock dynamic port) | `AcceptanceContext`, `TestcontainersConfiguration`, `HttpRiskScorerTest` |
| databases/containers | one Postgres and one Redis container per test JVM, Testcontainers-managed | `TestcontainersConfiguration` |
| Gradle build directory | per worktree (`build/` inside the worktree) | Gradle default |
| Gradle daemon | shared, safe for sequential use | single slice, no parallel cohort |
| Docker daemon | shared; containers are namespaced by Testcontainers session | Testcontainers |
| docker compose project | not used by tests | `compose.yaml` is for `bootRun` only |

### Authorization boundaries

Local branches, worktrees, commits and test runs: needed. Remote push of the feature branch and opening the PR:
needed for `ready-pr` and above. Merge into `main`: needed for `integration-merged`. Staging, production: not
applicable. The plan documents these; the operator's execution decision authorizes them.

## Slice map

| ID | Actor capability | Prerequisites | Parallel group | Owned risks and edge cases | Verification verdict / primary proof |
| --- | --- | --- | --- | --- | --- |
| S1 | The merchant can safely retry a capture or a reversal and get the same answer | none | A (single slice) | replay only in the target state; no second ledger entry or event; racing retry; conflicting transitions stay `409` | consumer-boundary E2E: Cucumber scenario over HTTP + focused unit tests |

## Vertical slices

### S1 - The merchant can safely retry a capture or a reversal

**Why now:** review finding; payments retries are certain, not hypothetical; the brief asks for idempotent POSTs.
**Prerequisites:** none.
**Parallel-safe with:** none (single slice).

#### Boundaries

- **In scope:** replay in `AuthorizationLifecycle.settle` when the re-loaded status equals the target status; unit tests for capture and reverse replays (ledger and outbox unchanged); acceptance scenario for the capture retry over HTTP; preserved `409` cases stay covered by the existing domain and controller tests.
- **Out of scope:** `Authorization` domain code, schema, response shape, `POST /v1/authorizations`, expiry rules, README/DECISIONS prose (closeout owns the decision record; the README API table sentence may be adjusted in the same `feat:` commit if the reviewer asks).
- **Likely change surface:** `src/main/java/pl/fairydeck/authorization/application/AuthorizationLifecycle.java`; `src/test/java/pl/fairydeck/authorization/application/AuthorizationLifecycleTest.java`; `src/acceptanceTest/resources/pl/fairydeck/authorization/acceptance/card_authorization.feature`; `src/acceptanceTest/java/pl/fairydeck/authorization/acceptance/CardAuthorizationSteps.java`.
- **Conflict footprint:** none (no other active work).
- **Worktree resource lease:** Gradle build dir of the slice worktree; Testcontainers session; no fixed ports.

#### Contracts

- **Consumes:** `AuthorizationRepository.findById`, `LedgerRepository.lock`, `Authorization.status()`, `Authorization.capture/reverse` (strict), `Outbox.record`.
- **Produces:** settlement endpoints idempotent for repeated identical requests: `200` + current representation, no side effects, for `capture` on `CAPTURED` and `reverse` on `REVERSED` or `EXPIRED`.

#### Behavior and acceptance

1. Merchant captures an approved authorization -> `200 CAPTURED`; ledger `HOLD, HOLD_RELEASE, CAPTURE`; one event.
2. Merchant repeats the capture -> `200 CAPTURED`, same id; ledger still three entries; still one event; settled `30.00`, available `70.00`.
3. Same for reverse on `REVERSED` (unit level).

- A second `capture(id)` on a `CAPTURED` authorization returns it without throwing; ledger size and outbox size unchanged.
- A second `reverse(id)` on a `REVERSED` authorization returns it without throwing; ledger size and outbox size unchanged.
- `reverse(id)` on an `EXPIRED` authorization returns it (status `EXPIRED`) without throwing; ledger size and outbox size unchanged.
- `capture(id)` on a `REVERSED` authorization still throws `IllegalStateException` (existing behavior; add the assertion if not present).
- Over HTTP: capturing twice leaves settled at `30.00` and available at `70.00`, and both capture answers carry the same id and status.

#### Test cycle

- **Verification verdict:** consumer-boundary E2E (API-only service; the Cucumber suite over HTTP is the product surface).
- **Expected initial RED:** `AuthorizationLifecycleTest.aRetriedCaptureIsAnsweredFromTheCurrentStateWithoutBookingAgain` fails with `IllegalStateException: Cannot capture on a CAPTURED authorization` (behavior missing, harness healthy: sibling tests pass); Cucumber scenario "Retrying a capture after it succeeded changes nothing" fails with `HttpClientErrorException$Conflict: 409` on the second capture step (application boots, first capture passes, so the failure is the missing behavior).
- **Focused tests:** the replay unit tests (capture on `CAPTURED`, reverse on `REVERSED`, reverse on `EXPIRED`), a preserved-conflict unit test (capture after reverse -> `IllegalStateException`), the new acceptance scenario.
- **Static analysis:** compile tasks with `-Werror` and `ArchitectureTest`, repository root.
- **Sonar:** not configured (`NOT_APPLICABLE`, evidence above).
- **Independent review focus:** replay decision happens after the re-load under the lock (not before); no event/ledger write on replay; capture after `EXPIRED`, cross transitions and `DECLINED` still `409`; reverse after `EXPIRED` replays; domain untouched; tests would fail if the replay check were removed (deliberate-break); commit convention respected.
- **E2E setup:** none beyond the suite (Testcontainers + in-process WireMock, low-risk stub).
- **E2E flow:** Given a card 100.00 GBP, an approved purchase of 30.00 GBP; When captured; And captured again; Then the second answer equals the first (id, status `CAPTURED`); settled 30.00; available 70.00.
- **Expected evidence:** Cucumber report under `build/reports/tests/acceptanceTest` and `build/test-results/acceptanceTest/*.xml`; unit XML under `build/test-results/test`.
- **Deliberate-break check:** with the replay check removed, both new tests must fail; record once in the worker report.
- **Commands:** focused `./gradlew test --tests '...AuthorizationLifecycleTest'`; static as above; E2E `./gradlew acceptanceTest`; full `./gradlew build`. No new commands introduced.
- **Post-merge integration check:** `./gradlew build` on the feature head; CI `build` on the PR head SHA.

#### Delivery safety

- **Observability:** unchanged request log line (`RequestLoggingFilter`) shows the replayed `200`.
- **Rollout:** none.
- **Rollback/fallback:** revert the `feat:` commit.

#### Executor handoff (subagent-ready)

> Implement only S1. Read `CLAUDE.md`, `src/main/java/pl/fairydeck/authorization/application/CLAUDE.md`, the nearest `.agents/project-context.md` files and this plan. Commit the failing tests first as `test: idempotent settlement retry specs`, observe and record the red build, then make them pass with the smallest change in `AuthorizationLifecycle` as `feat: replay retried captures and reversals`. Do not touch `Authorization`, the schema or the response records. Do not refactor unrelated code. Run the focused tests, the static checks and `./gradlew acceptanceTest`, then report changed files, commands, results, evidence paths and any plan assumption disproved by implementation.

## Dependency and concurrency audit

Single slice; no DAG. Integration order: S1 -> feature head verification -> PR -> CI -> rebase-merge into `main`.

## Implementation-orchestrator handoff

- **Canonical plan identity:** `context/plans/2026-09-08-idempotent-settlement-retries.md`; SHA-256 recorded in the run ledger and the reconciliation record after approval; source revision `37e3d118c1116bb2e28ae754a26156f7c44100eb`; status per the header.
- **Project-context entrypoint:** `context/map/INDEX.md`, `context/map/manifest.json` (snapshot `b4bef16`; freshness partial: stale only for lint flags and the skills bundle).
- **Execution DAG:** S1 only.
- **Feature integration:** base `main@37e3d11`; branch `feat/idempotent-settlement-retries`; GitHub PR; rebase and merge; post-slice `./gradlew build`.
- **Gate coverage:** acceptance RED -> focused tests -> static (`-Werror`, ArchUnit) -> Sonar `NOT_APPLICABLE` -> independent review -> consumer-boundary E2E (`acceptanceTest`) -> `./gradlew build` on the assembled SHA -> CI on the PR head.
- **Final delivery:** PR to `main`, CI green on the PR head, rebase-merge; no staging or production stage exists.
- **Expected authorization envelope:** local worktrees/commits; push feature branch; open PR; merge into `main` (for `integration-merged`).
- **Reconciliation record:** produced with the plan hash at approval for `project-context-initializer` federation (index and manifest refresh at closeout).

## Execution recommendation

| Decision | Recommendation | Evidence and trade-off |
| --- | --- | --- |
| Profile | `standard` | the change is tiny, but it sits on a payment and idempotency/concurrency boundary, which the quick-dev eligibility list excludes; with one slice the full chain costs one review and one E2E run |
| Orchestration | `fire-and-forget` | one slice, no external side effects before the PR; the PR and CI are the natural checkpoints |
| Terminal outcome | `integration-merged` | PR rebase-merged into `main` with CI green on the PR head; there is no staging or production stage |
| Model policy | `daily-coding`: worker and reviewer `sonnet` (resolved on the host to the current Sonnet identifier), medium effort; orchestrator = current model | routine change; independent reviewer in a separate context |
| Automatic escalation | allowed (no-op for `standard`) | n/a |

### Quick-profile eligibility

- **Verdict:** not eligible
- **Evidence:** payment boundary and idempotency/concurrency behavior; otherwise a two-file change with a healthy baseline.
- **Escalation triggers:** n/a under `standard`.

### Operator execution decision

**Status:** confirmed (2026-09-08, trusted conversation)

- **Selected profile:** `standard`
- **Selected orchestration:** `fire-and-forget`
- **Terminal outcome:** `integration-merged` — PR rebase-merged into `main` with the `build` workflow green on the PR head SHA
- **Selected worker/reviewer model policy:** `daily-coding`; worker `sonnet`, reviewer `sonnet` (resolved on the host to `claude-sonnet-5`), medium effort; orchestrator = current session model
- **Automatic profile escalation:** allowed
- **Challenge raised:** none; the planner recommended this contract
- **Operator override:** D5 (reverse after `EXPIRED` answers `200`), recorded above
- **Implementation authorization:** granted in the trusted conversation for this plan and this envelope (local worktrees/commits, push of the feature branch, PR, merge into `main`); no staging or production actions exist

## Coverage matrix

| Requirement, happy-path step, edge case, or risk | Slice | Focused test | Verification evidence |
| --- | --- | --- | --- |
| retried capture replays, books nothing | S1 | `AuthorizationLifecycleTest` replay-capture | Cucumber scenario: same answer, settled 30.00, available 70.00 |
| retried reverse replays, books nothing | S1 | `AuthorizationLifecycleTest` replay-reverse | unit |
| reverse after expiry replays, books nothing | S1 | `AuthorizationLifecycleTest` reverse-on-expired | unit |
| conflicting transitions still 409 | S1 (preserved) | `AuthorizationTest:99`, new unit assertion, controller 409 test | unit + slice |
| racing retry decided under the lock | S1 | design: check after re-load under `ledger.lock`; reviewer focus | review evidence |
| no second event | S1 | outbox size assertion | unit |

## Accepted risks

- No caller identity: any holder of an authorization id can settle or replay it. Owner: repository author (DECISIONS §15). Revisit when an API gateway or JWT is introduced.

## Approval

Approve this plan to move to the operator execution decision (profile, orchestration, terminal outcome, model policy,
escalation). Approval of the plan is not implementation authorization; the execution decision is recorded before the
first side effect.
