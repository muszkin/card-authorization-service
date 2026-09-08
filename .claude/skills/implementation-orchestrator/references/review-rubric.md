# Independent review rubric

The review gate is an adversarial, read-only inspection of an immutable candidate. It is separate from worker self-review.

Under `quick-dev` or `quick-bug-fix`, per-slice review may be recorded as `DEFERRED_TO_FINAL`; this is not a pass. The independent combined review must then inspect the complete assembled feature delta and explicitly cover every deferred slice concern before remote delivery.

## Independence

- Use an agent that did not implement the candidate.
- Do not ask the worker to review its own patch under a different label.
- Inspect from a separate clean read-only review worktree or detached checkout pinned to the candidate SHA, not from the worker's mutable worktree.
- Give the reviewer the plan and evidence, but not a leading claim that the implementation is correct.
- The reviewer does not edit code. It reports findings with evidence; the logical owner repairs them.

## Review package

Provide:

- plan path/hash and exact slice acceptance criteria;
- candidate base SHA and head SHA;
- exact `BASE...HEAD` diff or merge-base diff for combined review;
- owned and intentionally changed files;
- relevant architecture, ADR, contracts and nearest directory context;
- baseline and gate evidence;
- known preserved behaviors and explicit non-goals;
- migrations, API/event/schema changes and rollout constraints;
- commands the reviewer may run read-only.

The reviewer verifies the SHAs before reading the diff.

## Required dimensions

Inspect all relevant dimensions:

1. **Acceptance correctness** — every planned behavior is implemented and observable.
2. **Regression and preserved behavior** — existing paths, compatibility and consumers remain intact.
3. **Logical correctness** — invariants, branching, state transitions, calculations and error handling.
4. **Edge cases** — empty, invalid, duplicate, concurrent, reordered, retried, partial and boundary inputs.
5. **Security and privacy** — authorization, validation, injection, secrets, data exposure and trust boundaries.
6. **Data and migration safety** — backward/forward compatibility, rollback, locking, idempotency and mixed-version runtime.
7. **Concurrency and reliability** — races, retries, timeouts, duplicate delivery, transactions and failure recovery.
8. **Architecture and ADR compliance** — boundaries, dependencies, ownership and technology decisions.
9. **Operability** — logs, metrics, alerts, feature flags, rollout and diagnosability where required.
10. **Tests and E2E strength** — assertions can fail for the intended defect, cover the user journey and avoid false confidence.
11. **Scope and maintainability** — no unrelated changes, accidental generated output, dead code, stubs or hidden TODOs.

## Finding format

Each finding contains:

- stable ID;
- severity: `CRITICAL`, `HIGH`, `MEDIUM` or `LOW`;
- file and tight line/location;
- violated acceptance, invariant or preserved behavior;
- concrete failure scenario;
- evidence or reproduction command;
- required outcome, without dictating an unsafe patch;
- whether it blocks the gate.

`CRITICAL`, `HIGH` and material `MEDIUM` findings block. A `LOW` item blocks when it reveals a required acceptance or repository-policy violation. Do not use a numeric score or percentage to waive findings.

## Verdict

The reviewer returns exactly one verdict:

- `PASS` — no unresolved material findings for the inspected SHA;
- `FAIL` — one or more material findings;
- `BLOCKED_EXTERNAL` — evidence could not be inspected for an external reason.

Include the inspected base/head SHA and commands run. An empty report without scope/evidence is invalid.

## Re-review

After any repair:

1. invalidate the old verdict;
2. provide a fresh immutable diff including the repair;
3. ask an independent reviewer to verify both the finding resolution and the whole affected surface;
4. run downstream E2E only after the fresh review passes.

The same reviewer may re-review if independence is preserved, but a fresh reviewer is preferable after repeated disagreement or repeated failure.

## Combined feature review

After all slices integrate, conduct a new review of the entire feature delta from the integration target's merge base to feature HEAD. Slice passes do not prove combined correctness.

For quick profiles, include the list of per-slice review obligations marked `DEFERRED_TO_FINAL`, their acceptance criteria, risk surfaces and integration results. The combined reviewer must close each obligation explicitly; a generic whole-diff summary does not satisfy deferred review.

Emphasize:

- cross-slice contracts and integration order;
- feature-flag and migration rollout;
- interactions with untouched consumers;
- accumulated security/data risks;
- missing end-to-end paths;
- deviations between final code and the approved plan.
