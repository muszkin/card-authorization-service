# Worker report — S1: idempotent settlement retries

## Identity

- Branch: `feat/idempotent-settlement-retries-s1`
- Worktree: `/home/muszkin/work/zilch/worktrees/s1`
- HEAD before work: `37e3d118c1116bb2e28ae754a26156f7c44100eb` (expected base, confirmed)
- HEAD after work: `ddd71fada0e301552715ec135e4e3366dd328068`
- `git status --porcelain` at every guard checkpoint: empty (clean) except between the two commits, where exactly the expected files were modified.
- Identity guard was run at start, before each commit, and before the final report; all outputs matched the expected worktree/branch/base SHA at every checkpoint.

## Files changed

Commit `test: idempotent settlement retry specs` (`f0f62f4be99a43b7a388dc1c8689caa342e98c53`):
- `src/test/java/pl/fairydeck/authorization/application/AuthorizationLifecycleTest.java` — 4 new tests: `aRetriedCaptureIsAnsweredFromTheCurrentStateWithoutBookingAgain`, `aRetriedReverseIsAnsweredFromTheCurrentStateWithoutBookingAgain`, `reverseOnAnExpiredAuthorizationReturnsItWithoutBookingAgain`, `captureAfterReversalStillThrows`.
- `src/acceptanceTest/resources/pl/fairydeck/authorization/acceptance/card_authorization.feature` — new scenario `Retrying a capture after it succeeded changes nothing`.
- `src/acceptanceTest/java/pl/fairydeck/authorization/acceptance/CardAuthorizationSteps.java` — new steps `the capture is retried` (`transition("capture")`) and `the retry returns the same settled authorization` (compares last two entries of `answers`: same id, status `CAPTURED`).

Commit `feat: replay retried captures and reversals` (`ddd71fada0e301552715ec135e4e3366dd328068`):
- `src/main/java/pl/fairydeck/authorization/application/AuthorizationLifecycle.java` only. `Authorization.java` was read but **not** edited, matching the packet's boundary.

## Implementation

`settle(...)` now takes a third parameter, `Set<AuthorizationStatus> alreadySatisfiedBy`. Immediately after `ledger.lock(cardId)` and the second (authoritative) `load(...)`, if the re-loaded status is already in that set, `settle` returns the authorization unchanged — no transition call, no `ledger.append`, no `authorizations.save`, no `outbox.record`.

- `capture` uses `EnumSet.of(CAPTURED)`.
- `reverse` uses `EnumSet.of(REVERSED, EXPIRED)` (per D5 operator override: a reverse retry after the hold already expired also replays as `200`/`EXPIRED`, since the hold is released either way).
- `releaseExpiredHolds` uses `EnumSet.noneOf(AuthorizationStatus.class)` (empty set — no replay path there, unchanged strict behaviour).

`Authorization`, the schema, controllers, response records, build files, README and DECISIONS were not touched.

## RED evidence (before the fix)

Focused run: `./gradlew test --tests 'pl.fairydeck.authorization.application.AuthorizationLifecycleTest' --tests 'pl.fairydeck.authorization.domain.authorization.AuthorizationTest'`

```
AuthorizationLifecycleTest > aRetriedCaptureIsAnsweredFromTheCurrentStateWithoutBookingAgain() FAILED
    java.lang.IllegalStateException: Cannot capture on a CAPTURED authorization
    at Authorization.requireStatus(Authorization.java:105) / Authorization.capture(Authorization.java:78)

AuthorizationLifecycleTest > reverseOnAnExpiredAuthorizationReturnsItWithoutBookingAgain() FAILED
    java.lang.IllegalStateException: Cannot reverse on a EXPIRED authorization

AuthorizationLifecycleTest > aRetriedReverseIsAnsweredFromTheCurrentStateWithoutBookingAgain() FAILED
    java.lang.IllegalStateException: Cannot reverse on a REVERSED authorization

15 tests completed, 3 failed
```
`captureAfterReversalStillThrows` (the preserved-conflict assertion) passed unmodified, and every pre-existing `AuthorizationLifecycleTest`/`AuthorizationTest` test stayed green, proving the harness was healthy and the RED was exactly the missing behaviour. Full excerpt saved at `evidence/red-unit.txt`.

`./gradlew compileAcceptanceTestJava` was run and succeeded (new steps compile). Per instructions, `./gradlew acceptanceTest` was **not** run by this worker — the orchestrator runs the E2E gate. Expected outcome for the new scenario before the fix: it fails at the "the capture is retried" step with `HttpClientErrorException$Conflict` (409), since `capture` on an already-`CAPTURED` authorization threw before the fix.

## GREEN evidence (after the fix)

- Focused tests (`AuthorizationLifecycleTest` + `AuthorizationTest`): `BUILD SUCCESSFUL`.
- Full unit suite `./gradlew test`: `BUILD SUCCESSFUL`.

## Deliberate-break check

Temporarily removed the `if (alreadySatisfiedBy.contains(authorization.status())) { return authorization; }` block from `settle` in the working tree (uncommitted), re-ran `./gradlew test --tests 'pl.fairydeck.authorization.application.AuthorizationLifecycleTest'`: the same 3 new tests failed with the same `IllegalStateException` messages as the original RED (8 tests completed, 3 failed). This confirms the tests actually exercise the replay path and are not vacuously green. The change was then restored exactly (`git diff --stat` afterwards showed only `AuthorizationLifecycle.java`, 15 insertions / 4 deletions, matching the intended fix), and the focused tests were re-run green before committing. The broken state was never committed.

## Static gate (candidate SHA `ddd71fada0e301552715ec135e4e3366dd328068`)

- `./gradlew compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava compileAcceptanceTestJava` (`-Xlint:all,-serial -Werror`): `BUILD SUCCESSFUL`.
- `./gradlew test --tests 'pl.fairydeck.authorization.architecture.ArchitectureTest'`: `BUILD SUCCESSFUL`.
- Tails saved at `evidence/static.txt`.

## Commands run, in order

| # | Command | Result | Evidence |
|---|---|---|---|
| 1 | identity guard (`git rev-parse --show-toplevel/--abbrev-ref HEAD/HEAD`, `git status --porcelain`) | worktree/branch/base matched, clean | inline above, repeated at each checkpoint |
| 2 | `./gradlew test --tests 'pl.fairydeck.authorization.application.AuthorizationLifecycleTest' --tests 'pl.fairydeck.authorization.domain.authorization.AuthorizationTest'` (baseline) | BUILD SUCCESSFUL | `evidence/baseline.txt` |
| 3 | wrote failing tests + feature scenario + steps | — | see "Files changed" |
| 4 | same focused command (RED) | BUILD FAILED, 3 failures as expected | `evidence/red-unit.txt` |
| 5 | `./gradlew compileAcceptanceTestJava` | BUILD SUCCESSFUL | inline above |
| 6 | `git add` (3 test/feature/steps files only) + commit `test: idempotent settlement retry specs` | committed `f0f62f4` | `git log` |
| 7 | implemented the fix in `AuthorizationLifecycle.java` | — | see "Implementation" |
| 8 | same focused command (GREEN) | BUILD SUCCESSFUL | inline above |
| 9 | `./gradlew test` (full unit suite) | BUILD SUCCESSFUL | inline above |
| 10 | deliberate-break: removed check, focused test, restored, re-verified green | 3 failures reproduced, then green again | inline above |
| 11 | `git add` (`AuthorizationLifecycle.java` only) + commit `feat: replay retried captures and reversals` | committed `ddd71fa` | `git log` |
| 12 | `./gradlew compileJava compileTestFixturesJava compileTestJava compileIntegrationTestJava compileAcceptanceTestJava` | BUILD SUCCESSFUL | `evidence/static.txt` |
| 13 | `./gradlew test --tests 'pl.fairydeck.authorization.architecture.ArchitectureTest'` | BUILD SUCCESSFUL | `evidence/static.txt` |
| 14 | final identity guard + `git status --porcelain` + `git log --oneline -3` | clean, HEAD `ddd71fa` on `feat/idempotent-settlement-retries-s1` | inline above |

`./gradlew acceptanceTest` and `./gradlew build` were intentionally **not** run by this worker, per the packet's instructions (orchestrator's job).

## Commit SHAs

1. `test: idempotent settlement retry specs` — `f0f62f4be99a43b7a388dc1c8689caa342e98c53`
2. `feat: replay retried captures and reversals` — `ddd71fada0e301552715ec135e4e3366dd328068`

## Risks

- No caller identity on settlement endpoints — pre-existing accepted risk (DECISIONS §15), unaffected by this change.
- The replay check is a plain status-set membership test; if a future slice adds more terminal statuses without updating `CAPTURE_ALREADY_SATISFIED`/`REVERSE_ALREADY_SATISFIED`, those new statuses would fall through to the strict `IllegalStateException` path by default (fail-safe, not fail-open), which is the correct direction of failure for a payments boundary but worth flagging for the next slice that touches `AuthorizationStatus`.
- E2E (`acceptanceTest`) has not actually been run against this candidate by this worker; the orchestrator's E2E gate is the first real confirmation that the HTTP-level retry scenario passes as designed. Given the focused unit coverage and the identical mechanism used by `AuthorizationController`, risk of E2E failure is assessed as low, but it is unverified by this worker.

## Plan assumptions checked against implementation

No plan assumption was disproved. Specifically:
- The re-loaded status under the lock was confirmed to be the right (and only) place to decide replay — no second database round trip was added, `settle`'s existing double-load shape was reused as designed (D3).
- D5 (reverse-after-EXPIRED replays as `200`) implemented exactly as specified; capture-after-EXPIRED continues to throw (not in either replay set) — verified by the fact that no test or code path added `EXPIRED` to `CAPTURE_ALREADY_SATISFIED`.
- The preserved-conflict case (`capture` after `REVERSED` throws `IllegalStateException`) was verified to already pass before any production change, confirming the domain's existing strictness needed no modification.
