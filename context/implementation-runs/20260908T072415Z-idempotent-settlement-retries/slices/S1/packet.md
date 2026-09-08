# Slice packet S1 — the merchant can safely retry a capture or a reversal

Run `20260908T072415Z-idempotent-settlement-retries` · profile **standard** · orchestration fire-and-forget · terminal outcome integration-merged · worker model `sonnet` (resolved `claude-sonnet-5`, medium) · automatic escalation allowed.

## 1. Objective and user-visible value
A merchant that repeats `POST /v1/authorizations/{id}/capture` after a successful capture gets `200` with the same authorization (status `CAPTURED`), and nothing is booked twice. The same for `/reverse` on an authorization already `REVERSED` **or** `EXPIRED` (operator decision D5). Conflicting transitions (capture after reverse/expiry, reverse after capture, anything on DECLINED) keep answering `409`.

## 2. Plan
`/home/muszkin/work/zilch/card-authorization-service/context/plans/2026-09-08-idempotent-settlement-retries.md` (sha256 `ac3912ffe26bd6b816c2bb73b9d452177d071a6fe498f71d5fadfbd6c7bd5d50`). Read the sections "Solution contract", "Vertical slices / S1", "Global implementation constraints". Base SHA `37e3d118c1116bb2e28ae754a26156f7c44100eb`.

## 3. Acceptance scenarios and expected initial RED
- Unit (`AuthorizationLifecycleTest`): after `lifecycle.capture(id)`, a second `capture(id)` returns the authorization (status CAPTURED) without throwing; `ledger.entriesFor(card)` still has 3 entries; `outbox.recorded()` still has 1 event. Same shape for `reverse` on REVERSED (2 entries, 1 event) and for `reverse` on an EXPIRED authorization (make it expire through `lifecycleAt(afterExpiry).releaseExpiredHolds()`, then `reverse(id)` returns status EXPIRED, entries and events unchanged). Add one preserved-conflict assertion: `capture(id)` on a REVERSED authorization throws `IllegalStateException`.
- Acceptance (`card_authorization.feature`): "Retrying a capture after it succeeded changes nothing": Given a card 100.00 GBP and an approved purchase of 30.00 GBP; When the purchase is captured; And the capture is retried; Then the retry returns the same settled authorization (same id and status CAPTURED as the previous answer); And the settled amount is 30.00 GBP; And the available balance is 70.00 GBP. Add the two new steps to `CardAuthorizationSteps` (retry = call `transition("capture")` again; comparison = last two entries of `answers`).
- Expected RED before implementation: the unit tests fail with `IllegalStateException: Cannot capture on a CAPTURED authorization` (and the reverse variants); the scenario fails at the retry step with `HttpClientErrorException$Conflict`. Sibling tests must pass, which proves the harness is healthy.

## 4. Architecture / ADR constraints
DECISIONS.md §4 (lock before ledger reads), §8 (idempotency), §13 (captures after expiry refused), §14 (IllegalState -> 409). Domain transitions in `Authorization` stay strict and are NOT edited. The replay decision lives in `AuthorizationLifecycle.settle` **after** `ledger.lock(cardId)` and the second `load`: if the re-loaded status is one of the states that already satisfy the request, return the authorization without appending entries, saving or recording an event.

## 5. Instructions to honour
`/home/muszkin/work/zilch/card-authorization-service/CLAUDE.md`, `/home/muszkin/work/zilch/card-authorization-service/src/main/java/pl/fairydeck/authorization/application/CLAUDE.md`, `/home/muszkin/work/zilch/card-authorization-service/src/acceptanceTest/.agents/project-context.md`.

## 6. Context to read
`/home/muszkin/work/zilch/card-authorization-service/src/main/java/pl/fairydeck/authorization/application/.agents/project-context.md`, `/home/muszkin/work/zilch/card-authorization-service/context/map/architecture-and-flows.md` (section "Other flows").

## 7. Owned change surface and public contract
Owned: `src/main/java/pl/fairydeck/authorization/application/AuthorizationLifecycle.java`, `src/test/java/pl/fairydeck/authorization/application/AuthorizationLifecycleTest.java`, `src/acceptanceTest/resources/pl/fairydeck/authorization/acceptance/card_authorization.feature`, `src/acceptanceTest/java/pl/fairydeck/authorization/acceptance/CardAuthorizationSteps.java`. Forbidden: everything else (domain, schema, build files, README, DECISIONS, context/).
Contract produced: settlement endpoints idempotent for repeated identical requests as described in §1. No response-shape change.

## 8. Dependencies and consumers
None. `AuthorizationController` is unchanged and keeps returning `200` with `AuthorizationResponse.from(...)`.

## 9. Commands (working directory: `/home/muszkin/work/zilch/worktrees/s1`)
- baseline / focused: `./gradlew test --tests 'pl.fairydeck.authorization.application.AuthorizationLifecycleTest' --tests 'pl.fairydeck.authorization.domain.authorization.AuthorizationTest'`
- static: `./gradlew compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava compileAcceptanceTestJava` then `./gradlew test --tests 'pl.fairydeck.authorization.architecture.ArchitectureTest'`
- E2E (do NOT run it yourself; the orchestrator runs `./gradlew acceptanceTest` after the independent review): only make sure `compileAcceptanceTestJava` passes.
- full: `./gradlew build` is run by the orchestrator.

## 10. Gate requirements
static (compiler with -Werror + ArchUnit) must be green on the candidate SHA; Sonar NOT_APPLICABLE (not configured); independent review and E2E are run by the orchestrator on your immutable candidate SHA.

## 11. Cadence
standard: no deferred gates.

## 12–13. Models, mode, escalation
Worker `sonnet`; fire-and-forget; escalation not applicable to standard. If you discover the change needs to touch the domain or the schema, STOP and report (plan-defect), do not widen the scope.

## 14. Resource lease
L1: this worktree, its `build/`, the Testcontainers session of your JVMs. Do not run docker compose. Do not touch `/home/muszkin/work/zilch/card-authorization-service` (the user's checkout) or `/home/muszkin/work/zilch/worktrees/feature`.

## 15. Integration test after merge
`./gradlew build` on the feature head (orchestrator).

## 16. Prohibitions and authorization boundaries
Do not push, merge, open PRs, rebase, amend, squash, force anything, or edit files outside the owned surface. Do not add dependencies. Do not add tooling trailers to commits. Do not write to the run directory except `/home/muszkin/work/zilch/card-authorization-service/context/implementation-runs/20260908T072415Z-idempotent-settlement-retries/slices/S1/worker-report.md` and `/home/muszkin/work/zilch/card-authorization-service/context/implementation-runs/20260908T072415Z-idempotent-settlement-retries/slices/S1/evidence/`.

## 17. Required worker report (`/home/muszkin/work/zilch/card-authorization-service/context/implementation-runs/20260908T072415Z-idempotent-settlement-retries/slices/S1/worker-report.md`)
Branch, worktree, identity-guard outputs (`git rev-parse --show-toplevel`, branch, HEAD before/after), files changed, the RED evidence (exact failing assertion/exception excerpts, saved under `evidence/`), the two commit SHAs (`test:` then `feat:`), commands run with status and evidence paths, the deliberate-break note (with the replay check removed both new tests fail — do this in the working tree, then restore, do not commit it), risks, and any plan assumption disproved.
