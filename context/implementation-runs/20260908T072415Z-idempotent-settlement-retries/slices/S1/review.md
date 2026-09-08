# Independent review — S1 (idempotent settlement retries)

**Reviewer:** reviewer-S1 (independent, read-only; did not implement the candidate)
**Review worktree:** `/home/muszkin/work/zilch/worktrees/review-s1` (detached, read-only)
**Base SHA:** `37e3d118c1116bb2e28ae754a26156f7c44100eb`
**Head SHA (candidate):** `ddd71fada0e301552715ec135e4e3366dd328068`
**Commit pair:** `f0f62f4` `test: idempotent settlement retry specs` -> `ddd71fa` `feat: replay retried captures and reversals`

## Verification of scope

```
cd /home/muszkin/work/zilch/worktrees/review-s1
git rev-parse HEAD                     -> ddd71fada0e301552715ec135e4e3366dd328068
git status --porcelain                 -> (empty)
git diff 37e3d11..ddd71fa              -> 4 files, +84/-4 (see below)
git log --format='%h %s%n%b' 37e3d11..HEAD
```

Changed files (verified via `git diff --stat`):
- `src/acceptanceTest/java/pl/fairydeck/authorization/acceptance/CardAuthorizationSteps.java` (+13)
- `src/acceptanceTest/resources/pl/fairydeck/authorization/acceptance/card_authorization.feature` (+9)
- `src/main/java/pl/fairydeck/authorization/application/AuthorizationLifecycle.java` (+15/-4)
- `src/test/java/pl/fairydeck/authorization/application/AuthorizationLifecycleTest.java` (+47)

`Authorization.java` (domain) is **not** in the diff — confirmed untouched, matching the plan's constraint.

## Commands run

```
./gradlew test --tests 'pl.fairydeck.authorization.application.AuthorizationLifecycleTest'   -> BUILD SUCCESSFUL
./gradlew compileAcceptanceTestJava                                                            -> BUILD SUCCESSFUL
./gradlew test --tests 'pl.fairydeck.authorization.domain.authorization.AuthorizationTest'    -> BUILD SUCCESSFUL
./gradlew clean compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava compileAcceptanceTestJava
                                                                                                 -> BUILD SUCCESSFUL (clean rebuild, -Xlint:all,-serial -Werror per build.gradle.kts:23)
./gradlew test --tests 'pl.fairydeck.authorization.architecture.ArchitectureTest'              -> BUILD SUCCESSFUL, 5/5 passed
./gradlew test                                                                                  -> BUILD SUCCESSFUL, 106 tests total (102 baseline + 4 new), 0 failures/errors
```

`./gradlew acceptanceTest` was **not** run (Docker-gated; explicitly excluded from this reviewer's remit — the orchestrator runs it as the next gate). Instead, the new scenario's step implementations were read and traced by hand (see F3-INFO below) to confirm they would fail without the fix and pass with it.

I did not edit, stash, or checkout any other commit in this worktree; `git status --porcelain` was empty at both start and end of the review.

## Logical trace of the change

`AuthorizationLifecycle.settle` (private, in `AuthorizationLifecycle.java`) now takes a third parameter, a `Set<AuthorizationStatus> alreadySatisfiedBy`:

```java
private Authorization settle(UUID authorizationId,
        BiFunction<Authorization, Instant, List<LedgerEntry>> transition, Set<AuthorizationStatus> alreadySatisfiedBy) {
    ledger.lock(load(authorizationId).cardId());
    Authorization authorization = load(authorizationId);
    if (alreadySatisfiedBy.contains(authorization.status())) {
        return authorization;
    }
    Instant now = clock.instant();
    transition.apply(authorization, now).forEach(ledger::append);
    authorizations.save(authorization);
    outbox.record(AuthorizationEvent.of(authorization, now));
    return authorization;
}
```

- `capture` passes `CAPTURE_ALREADY_SATISFIED = {CAPTURED}`.
- `reverse` passes `REVERSE_ALREADY_SATISFIED = {REVERSED, EXPIRED}`.
- `releaseExpiredHolds` passes `NEVER_ALREADY_SATISFIED = {}` (empty), so `expire`'s behavior is bit-for-bit unchanged — confirmed by inspection, not just claim.

The replay check sits after both `ledger.lock(...)` and the second `load(...)` — i.e. after the authoritative post-lock read, exactly as D3 and the "independent review focus" line in the plan require. Any status not in the relevant set falls through unconditionally into `transition.apply(...)`, which calls the untouched domain method (`Authorization.capture`/`reverse`), which still throws `IllegalStateException` via `requireStatus(APPROVED, ...)` for every other state. This is why capture-after-REVERSED, capture-after-EXPIRED, reverse-after-CAPTURED, and anything-after-DECLINED all still throw — none of those statuses appear in either `EnumSet`, verified by reading the two literals, not by executing every combination.

`ApiExceptionHandler` and `AuthorizationController` are unmodified (checked byte-for-byte against the diff: neither file appears in it), so `IllegalStateException` -> 409 and the 200 success path are unchanged plumbing.

## Dimensions inspected

1. **Acceptance correctness** — implemented: replay for capture-on-CAPTURED, reverse-on-REVERSED, reverse-on-EXPIRED (D5); preserved 409 for every other non-APPROVED state. Verified by code trace and by the 4 new unit tests plus the new Cucumber scenario, all green.
2. **Regression / preserved behavior** — `Authorization.java` untouched; full `./gradlew test` run shows 106/106 green (102 baseline + 4 new), including the pre-existing domain invariant test `AuthorizationTest.allowsEachAuthorizationToBeSettledOnlyOnce` and the controller-level 409-mapping test (which mocks `AuthorizationLifecycle` and is structurally unaffected by this change).
3. **Logical correctness** — EnumSet membership per operation is correct and minimal; check placement (post-lock, post-reload) matches the plan's D3 exactly; no new `now`/domain-transition path was altered.
4. **Edge cases** — capture-on-CAPTURED, reverse-on-REVERSED, reverse-on-EXPIRED, capture-on-REVERSED (throws) all unit-tested; capture-on-EXPIRED and reverse-on-DECLINED/capture-on-DECLINED are covered by set non-membership plus the pre-existing domain/controller tests, not by a new dedicated application-level test (see F2-LOW). Racing retry is a design argument (lock ordering), not a new concurrency test, consistent with the plan's explicit test-cycle scope ("design: check after re-load under ledger.lock; reviewer focus").
5. **Security and privacy** — no new trust boundary, no new caller-identity check added or removed; accepted risk (DECISIONS §15) unchanged; no secrets touched.
6. **Data and migration safety** — no schema change, no migration; replay path performs zero writes (no `ledger.append`, no `authorizations.save`, no `outbox.record`), verified both by reading the code and by the unit tests asserting ledger/outbox sizes stay constant across a replay.
7. **Concurrency and reliability** — the existing `ledger.lock(cardId)` + second `load` pattern (pre-existing, per `JdbcLedgerRepository.lock` and DECISIONS §4, READ COMMITTED + `pg_advisory_xact_lock`) is what makes the post-lock read authoritative for a racing retry; the new check is placed after both, so a racing retry is decided from a fresh, lock-serialized read. No new round trip added (still exactly two `load` calls, as before).
8. **Architecture / ADR compliance** — domain stays framework-free and untouched; no new Spring beans; constructor injection unaffected (no constructor changed); layering unaffected (`application` still doesn't import `adapter`). Consistent with DECISIONS §4, §8, §13, §14.
9. **Operability** — no new logging/metrics needed or removed; existing request-log line covers the replayed 200 as before (per plan, unchanged).
10. **Tests and E2E strength** — traced the failure mode without the check: for all three new unit tests, removing the `alreadySatisfiedBy` short-circuit would route the second call into the domain's `capture`/`reverse`, which throws `IllegalStateException` on non-APPROVED status — this is an *unhandled* exception in the test body (no `assertThatThrownBy` wrapper), so all three tests would error out, not silently pass. For the acceptance scenario, `CardAuthorizationSteps.transition` calls `.retrieve()` on a plain `RestClient` with no `onStatus` handler, so a 409 response throws `HttpClientErrorException$Conflict`, failing the "the capture is retried" step. Confirms the deliberate-break claim by reasoning over the diff, as instructed (no file edits made to actually execute the break).
11. **Scope and maintainability** — diff touches exactly the 4 files the plan predicted, no unrelated changes, no dead code, no TODOs, no stubs. Commit messages are Conventional Commits in English with no tooling trailers (verified via `git log`, both commit bodies inspected in full). Compiler is clean under `-Xlint:all,-serial -Werror` across all source sets (`compileJava`, `compileTestJava`, `compileTestFixturesJava`, `compileIntegrationTestJava`, `compileAcceptanceTestJava`) after a clean rebuild.

## Findings

### F1-LOW — cosmetic line length on 2 new/modified lines
- **Severity:** LOW
- **Location:** `src/main/java/pl/fairydeck/authorization/application/AuthorizationLifecycle.java:29` (119 chars) and `:66` (124 chars, the `settle` signature line)
- **Violated criterion:** none stated explicitly; no checkstyle/line-length rule is configured in the repository (`grep` for `checkstyle`/`lineLength` in `build.gradle.kts` and a search for a checkstyle config file both came back empty).
- **Failure scenario:** none — purely stylistic.
- **Evidence:** a scan of the whole `src/main/java` tree for lines over 118 chars found 33 pre-existing lines already up to 133 chars long, so this is consistent with (not worse than) the existing codebase.
- **Required outcome:** none required; optional wrap if a future style pass touches this file.
- **Blocks:** no.

### F2-LOW — no explicit application-level unit test for reverse-after-CAPTURED
- **Severity:** LOW
- **Location:** `src/test/java/pl/fairydeck/authorization/application/AuthorizationLifecycleTest.java` (missing test, symmetric to the existing `captureAfterReversalStillThrows` at line 120)
- **Violated criterion:** plan's edge-case table lists "capture after REVERSED / reverse after CAPTURED -> 409 (unchanged)" as one preserved-behavior line; the diff adds a unit test for the capture side but not the reverse side at the `AuthorizationLifecycle` level.
- **Concrete failure scenario:** if a future change accidentally added `CAPTURED` to `REVERSE_ALREADY_SATISFIED`, no test in this file would catch it (the domain-level test `AuthorizationTest.allowsEachAuthorizationToBeSettledOnlyOnce` would still catch it only if the application layer still delegated to the domain transition for that case, which it currently does).
- **Evidence:** `REVERSE_ALREADY_SATISFIED = EnumSet.of(AuthorizationStatus.REVERSED, AuthorizationStatus.EXPIRED)` correctly excludes `CAPTURED` today (verified by inspection and by the fact `./gradlew test` is green), so this is a coverage-completeness gap, not a present defect.
- **Required outcome:** consider adding a mirror unit test (`reverseAfterCaptureStillThrows`) for defense-in-depth; not required to unblock this slice given the domain-level guarantee and the direct code inspection above.
- **Blocks:** no.

### F3-INFO — real-surface E2E (`./gradlew acceptanceTest`) not executed by this review
- **Severity:** informational, not a finding against the candidate
- **Location:** n/a
- **Note:** per the review instructions, `./gradlew acceptanceTest` requires Docker and is explicitly reserved for the orchestrator's next gate. This reviewer verified `./gradlew compileAcceptanceTestJava` succeeds and traced the new step definitions (`theCaptureIsRetried` -> `transition("capture")` -> plain `RestClient.retrieve()` with no error handler) to confirm the scenario is wired to fail on a 409 and pass on a 200, and that `theRetryReturnsTheSameSettledAuthorization` correctly compares the last two recorded answers (the two capture calls), not the original approval. This is not a substitute for actually running the suite; flagging so the orchestrator does not skip that gate.
- **Blocks:** no (not a material finding; it's a scope boundary, not a defect).

No CRITICAL, HIGH, or MEDIUM findings.

## Verdict

**PASS**

Rule applied: per the rubric, "`PASS` — no unresolved material findings for the inspected SHA." All findings raised are LOW and none reveal a required acceptance or repository-policy violation (rubric: "A `LOW` item blocks when it reveals a required acceptance or repository-policy violation" — F1 is cosmetic with no enforced convention, F2 is a defense-in-depth suggestion with existing equivalent coverage at the domain layer, F3 is a scope note, not a defect). All 11 rubric dimensions were inspected; the deliberate-break claim, the post-lock/post-reload check placement, the untouched domain, the preserved 409 paths, the commit-pair convention, and the `-Werror` clean compile were each independently verified rather than assumed.
