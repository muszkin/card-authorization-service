# Slice quality and repair loop

Each slice proves behavior under the selected execution profile. A red gate routes back to implementation; it does not end the run. Quick profiles may defer named heavy slice gates to final feature verification, but they never convert deferred work into green evidence.

## Full-profile state sequence

```text
CONTEXT_LOADED
  -> BASELINE_PROVEN
  -> ACCEPTANCE_RED_PROVEN
  -> IMPLEMENTED
  -> STATIC_GREEN
  -> SONAR_ACCOUNTED
  -> REVIEW_GREEN
  -> E2E_GREEN
  -> READY_FOR_INTEGRATION
```

Focused unit/component/integration tests run during implementation and before static analysis, even though they are not separate release gates in this diagram.

For `quick-dev` and `quick-bug-fix`, use:

```text
CONTEXT_LOADED
  -> BASELINE_PROVEN
  -> EXPECTED_BEHAVIOR_PROVEN
  -> IMPLEMENTED
  -> FOCUSED_GREEN
  -> CHEAP_STATIC_GREEN
  -> HEAVY_GATES_DEFERRED_TO_FINAL
  -> QUICK_READY_FOR_INTEGRATION
```

`HEAVY_GATES_DEFERRED_TO_FINAL` is not a quality verdict. It is a complete list of open Sonar, independent-review and real-surface E2E obligations that the orchestrator must close on the assembled feature SHA.

## 1. Prove the baseline

Start the application and test harness using repository-defined commands. Show that the planned user surface can reach the point at which the missing behavior will be asserted.

Separate:

- pre-existing product/test failures;
- environment or harness failures;
- the deliberate acceptance failure for the missing behavior.

An application that cannot boot or a browser that cannot connect is not a valid TDD RED.

## 2. Prove expected behavior before editing

For `standard` and `quick-dev`, create or identify the smallest meaningful acceptance scenario and capture the missing behavior before product implementation. Prefer real-surface RED; for a genuinely tiny `quick-dev` change, a focused characterization or regression assertion is acceptable when it directly detects the intended behavior and full real-surface E2E remains scheduled for final verification.

For `bug` and `quick-bug-fix`, reproduce the reported defect before product implementation at the highest practical boundary. Capture inputs, environment, actual result, expected result and evidence that distinguishes the product defect from stale data, incorrect setup, harness failure or an external outage. If the defect cannot be reproduced and no equivalent failing characterization is justified, do not guess a fix; investigate or return the plan to clarification.

For a RED acceptance scenario, capture:

- command and working directory;
- base commit SHA;
- acceptance test path plus a SHA-256 of its content or a stable patch/tree hash for the exact test used;
- scenario and assertion;
- failure excerpt or report path;
- explanation of why it fails specifically because the behavior is absent.

The final passing acceptance test must match that recorded artifact or be a documented strengthening. If its assertion, fixture, control flow or observable meaning changes after RED, apply the revised test to a disposable worktree at the pinned base and prove the intended RED again before accepting its GREEN.

If it passes before implementation, investigate whether the behavior already exists, the test is stale, or the plan is wrong. Do not manufacture failure by breaking unrelated setup.

For a refactor with no behavior change, use a characterization test and, in a disposable state, prove the test detects a relevant deliberate break or mutation.

## 3. Implement the vertical behavior

Implement the smallest complete path through all required layers. Follow the nearest `.agents/project-context.md`, ADRs and existing boundaries. Keep public contract and migration compatibility explicit.

Use focused tests continuously. Stage only named files. Do not include generated noise or unrelated cleanup.

## 4. Create an immutable candidate

After focused checks pass, inspect the worktree, stage only named intentional files and create a private candidate commit using the repository's English commit convention and ticket format where applicable. This commit exists so static analysis, Sonar, review and E2E can all bind to an immutable SHA; it is not a readiness verdict and must not be pushed or integrated by the worker.

Each repair creates a new candidate commit rather than rewriting away evidence from an inspected SHA. Record the candidate lineage. The orchestrator may later squash according to repository policy, but any rewritten/integrated SHA must receive the checks required by its changed parent or content.

## 5. Run the profile-specific gates

For one immutable candidate SHA in `standard` or `bug`, run:

1. focused tests/build/type checks;
2. static analysis;
3. Sonar analysis and actual Quality Gate;
4. independent review;
5. real-surface E2E.

The exact definitions and evidence contract are in [quality-gates.md](quality-gates.md). The reviewer contract is in [review-rubric.md](review-rubric.md).

For `quick-dev` or `quick-bug-fix`:

1. run focused regression/acceptance tests and the smallest truthful build/type checks;
2. run repository static checks that are cheap and scoped enough for fast feedback;
3. repair any failure until these checks pass on the candidate SHA;
4. record permitted per-slice Sonar, independent review and real-surface E2E as `DEFERRED_TO_FINAL`, including their exact final commands or dispositions;
5. integrate only under the orchestrator's quick-profile contract and immediately run the planned feature-head integration check.

If a quick slice exposes a protected/high-risk surface, cross-service contract, migration, broad change footprint, non-isolatable resource, unclear regression, or failure requiring deeper diagnosis, emit `PROFILE_ESCALATED` and move `quick-dev -> standard` or `quick-bug-fix -> bug` (or `standard` when the work is not actually a defect). Reissue the packet and run the newly required gates; never silently remain quick.

## Gate invalidation

Any product-code, test, build, configuration, schema or generated-artifact change invalidates the changed gate and every downstream gate:

| Failure repaired | Required rerun order |
| --- | --- |
| focused tests/build | focused -> static -> Sonar -> review -> E2E |
| static | focused -> static -> Sonar -> review -> E2E |
| Sonar | focused -> static -> Sonar -> review -> E2E |
| review | focused -> static -> Sonar -> fresh review -> E2E |
| E2E | focused -> static -> Sonar -> fresh review -> E2E |

For quick slices, any change invalidates focused and cheap-static evidence plus all `DEFERRED_TO_FINAL` obligation bindings. The final combined chain always starts fresh from the assembled feature SHA; a per-slice pass cannot substitute for it.

When Sonar is `DEFERRED_TO_CI`, retain that open obligation through local review and E2E. It closes only when the required Sonar check passes on the current integration PR head SHA; a new push makes the earlier result stale.

Documentation-only changes may preserve code gates only when they do not change commands, contracts, generated inputs or acceptance meaning. Record the reasoning.

## Failure classification

Classify every RED before assigning the repair:

- `introduced-by-slice` — the candidate caused the failure;
- `pre-existing-baseline` — reproduced unchanged at the pinned base;
- `cross-slice-contract` — integrated slices disagree at a boundary;
- `flaky-or-environmental` — timing, resource or harness instability with evidence;
- `external-service` — required remote dependency is unavailable;
- `tooling-unavailable` — required scanner/runtime cannot execute;
- `plan-defect` — current evidence materially contradicts an acceptance or architecture assumption.

Classification selects a route; it never converts a required failed gate to passed.

## Root-cause repair protocol

For each red gate:

1. reproduce it with the narrowest truthful command;
2. capture logs, screenshots, traces or reports;
3. trace backward from the failing boundary through data/control flow;
4. state one falsifiable root-cause hypothesis;
5. make the smallest coherent fix;
6. run the focused regression proof;
7. restart the invalidated ordered gates.

When the same failure signature repeats, do not apply the same speculative patch. Change strategy or assign a fresh investigator to challenge the hypothesis. There is no arbitrary retry quota for an in-scope fix.

For a failure that survives one repair attempt, or any defect whose cause is unknown, run the full method in [`systematic-debugging`](../../systematic-debugging/SKILL.md) rather than iterating here. It owns reproduction, the ruled-out ledger, bisection and the proof that the identified cause is the actual one, and it returns a repair packet to the logical owner.

## E2E repair discipline

If E2E is red:

- preserve screenshots, video, trace and console/network evidence when supported;
- decide whether the failure is product, test, environment or data;
- fix the root cause in the slice worktree;
- rerun all invalidated gates, including a fresh independent review;
- run the acceptance scenario again from a controlled state.

Do not loosen assertions, add blind sleeps or retry until green without explaining the nondeterminism.

## Ready-for-integration criteria

A slice is ready only when:

- the active profile's pre-edit proof was valid and recorded: acceptance RED for full feature work, focused missing-behavior proof for eligible `quick-dev`, or verified defect reproduction for bug profiles;
- one immutable private candidate commit contains only the intended slice changes;
- every locally runnable required gate passes for the same head SHA;
- any Sonar result marked `DEFERRED_TO_CI` is registered as an unresolved PR obligation and is not described as green;
- all material review findings are resolved and re-reviewed;
- the worktree contains only intentional files;
- the worker report matches the actual commit;
- the integration test and possible contract consumers are named.

For a quick profile, replace the full-gate criteria above with `QUICK_READY_FOR_INTEGRATION` only when focused tests and required cheap checks pass on the immutable candidate SHA, every omitted heavy gate is explicitly `DEFERRED_TO_FINAL`, quick eligibility still holds, and the post-integration check is named. Never report that state as slice-green or release-ready.
