---
name: systematic-debugging
description: Find the root cause of a defect before anything is changed. Reproduces the reported failure at the highest practical boundary, separates a real product defect from harness, environment, data, or external failure, narrows it by bisecting history, input, configuration, and dependencies, proves one falsifiable hypothesis at a time, and produces a minimal reproduction plus a regression test that fails for the right reason. Use for a reported bug, a production incident, a failing test nobody understands, a flaky suite, or a repair loop that has failed the same way twice. Do not use to implement a feature, to guess a fix without reproduction, or to re-run a gate that is simply red for a known reason.
---

# Systematic Debugging

Establish what is actually broken and why, with evidence, before any product code changes.

A fix applied to an unreproduced defect is a guess. It may make the symptom disappear, which is the worst outcome, because the defect stays and the evidence is gone.

Use the user's language unless the repository has an established documentation language. Keep observed reality separate from inference, proposals, and user decisions.

## Operating contract

- Do not edit product code before the defect is reproduced or explicitly classified as not reproducible with evidence. This holds inside a repair loop as well as outside one.
- Track two independent axes: provenance (`Observed`, `User-confirmed`, `Inferred`, `Unknown`, `Contradiction`) and decision state (`N/A`, `Proposed`, `Undecided`, `Decided`, `Accepted risk`, `Out of scope`). A stack trace is `Observed`; a theory about what it means is not.
- Change one variable per experiment and record what each result ruled out. An investigation that changed several things and then worked has explained nothing.
- Distinguish the symptom from the cause, and the cause from the trigger. Fixing where the error surfaced is the standard way to move a defect somewhere quieter.
- When the same failure signature repeats, change strategy rather than repeating the patch. There is no retry quota for an in-scope fix, but there is a limit on repeating a disproved hypothesis.
- A defect is not understood until you can explain why it did not happen earlier and why it happens now.
- Do not loosen an assertion, add a sleep, or retry until green. Nondeterminism is a finding to explain, not a nuisance to suppress.
- Investigation is read-mostly. Diagnostic instrumentation and a failing regression test are its only product-tree outputs, and both belong in an isolated worktree.
- Do not deploy, migrate, restart a shared service, or mutate production data to test a hypothesis without explicit authorization for that exact action.
- Adapt to the environment. Without version control, skip history bisection and say so rather than stopping; without an isolated worktree, use a disposable copy of the working tree and never experiment in the user's checkout; without access to the failing environment, narrow on what is reachable and name the evidence you could not obtain.

Read each reference completely before the phase it governs:

- [references/reproduction.md](references/reproduction.md) before attempting to reproduce anything.
- [references/isolation-and-bisect.md](references/isolation-and-bisect.md) before narrowing a reproduced failure.

Read [examples/worked-example.md](examples/worked-example.md) when the expected shape of a finished investigation is unclear. It is illustrative, not evidence.

Classify every failure using the shared taxonomy in [the orchestrator's slice quality loop](../implementation-orchestrator/references/slice-quality-loop.md) so a defect found inside a run and one found outside it are routed the same way.

## State machine

```text
REPORT_INTAKE
  -> REPRODUCED | NOT_REPRODUCIBLE
  -> CLASSIFIED
  -> NARROWED
  -> HYPOTHESIS
  -> EXPERIMENT
  -> ROOT_CAUSE_PROVEN
  -> REGRESSION_PROVEN
  -> HANDOFF
```

`EXPERIMENT` returns to `HYPOTHESIS` on a disproved theory, and that is the normal path. Returning to `HYPOTHESIS` three times with no narrowing means the failure is not where you are looking; return to `NARROWED` and widen the search.

`NOT_REPRODUCIBLE` is a terminal state with a report, not a failure. It names what was tried, what evidence was unavailable, and what would make the defect reproducible.

## 1. Intake the report

Establish what was actually observed, separately from what was concluded.

Record:

- the exact expected behavior and the exact actual behavior, in the reporter's terms;
- who or what observed it, on which surface, at what time, in which environment;
- the revision, build, artifact digest, or deployed version involved;
- inputs, identities, and state, including anything that distinguishes the failing case from working ones;
- raw evidence: logs, stack traces, screenshots, traces, request identifiers, exit codes;
- whether it is reproducible for the reporter, and how often.

Strip interpretation out of the report. "The cache is broken" is a hypothesis; the observation underneath it is what matters. Ask for the missing observation rather than adopting the reporter's diagnosis.

Check whether the repository already knows about this: existing issues, a known-flaky list, a prior investigation, or an accepted risk recorded in an ADR or closeout.

## 2. Reproduce

Follow the reproduction reference. Reproduce at the highest boundary that still shows the failure, then work inward.

A reproduction is valid only when it fails for the reported reason. A test that fails because the harness cannot start, the fixture is missing, or the service is unreachable is an environment failure and proves nothing about the product.

Record the reproduction as a command plus a starting state that a different person on a different machine can run.

If the defect cannot be reproduced, enter `NOT_REPRODUCIBLE` and report it honestly, including the environments tried and the evidence that could not be obtained. Do not fix something you cannot see.

## 3. Classify the failure

Before narrowing, decide what kind of failure this is: a product defect, a pre-existing baseline failure, a contract disagreement between components, a flaky or environmental failure, an external service failure, or unavailable tooling.

Classification selects the route. It never converts a red gate into a passed one, and it never ends the investigation on its own: "environmental" is a claim that requires the same evidence as any other.

## 4. Narrow

Follow the isolation reference. Reduce the failing case along whichever axes apply: history, input, configuration, dependency version, data, concurrency, and environment.

Maintain a ruled-out ledger as you go. What an experiment eliminated is as valuable as what it confirmed, and it is what stops the investigation circling.

Stop narrowing when the failing case is minimal: removing anything else makes the failure disappear.

## 5. Form and test one hypothesis

State a hypothesis that could be wrong: "the failure happens because X, therefore changing only Y will change the outcome in this specific way."

Design the smallest experiment that distinguishes it from the alternatives. Predict the result before running it. An experiment whose outcome you cannot predict either way is not testing a hypothesis.

Run it, record the result against the prediction, and update the ledger. A disproved hypothesis is progress; a hypothesis quietly abandoned after an ambiguous result is not.

## 6. Prove the root cause

The root cause is proven when you can do all three:

1. explain the mechanism from the trigger to the observed symptom, through the actual code path;
2. make the defect appear and disappear by changing only the identified cause;
3. explain why it did not surface earlier, or why the earlier conditions hid it.

If you can only do the second, you have found a lever, not a cause. Keep going, or record explicitly that the fix is empirical and name the residual risk and its owner.

## 7. Prove the regression test

Write the test that would have caught this, at the boundary where the defect actually lives.

Prove it fails on the unfixed revision for the reported reason, and record that failure. A regression test first run after the fix proves only that the code currently passes it.

Cover the minimal reproduction plus the nearest edge case that shares the cause. Do not add a broad suite; add the assertion that fails for this defect.

## 8. Hand off

Investigation ends at a proven cause and a failing regression test. The fix belongs to the delivery path:

- inside an `implementation-orchestrator` run, return the classification, minimal reproduction, root cause, and regression proof as a repair packet to the logical owner;
- outside a run, recommend the `quick-bug-fix` profile for a narrow, low-blast-radius defect and the `bug` profile when the cause crosses boundaries, touches data, security, concurrency, or migrations, or is not fully proven;
- when the evidence contradicts the plan or the architecture rather than the code, route it to `implementation-planning` as a plan defect instead of patching around it.

Recommend `task-closeout` when the investigation produced a durable lesson, a workaround with a trigger, or an assumption that turned out wrong.

## Non-interactive runs

In a non-interactive run, complete reproduction, classification, narrowing, and hypothesis testing within the authorized surface, and stop before any action requiring authorization you do not have, such as touching a shared environment. Report the ruled-out ledger and the exact authorization needed. Do not guess a cause to produce a conclusion.

## Completion report

Report a separate verdict per dimension using `complete`, `partial`, `blocked`, or `not-applicable` with a reason:

- reproduction, including the boundary and its fidelity to the report;
- classification, with its evidence;
- narrowing, with the ruled-out ledger;
- root cause, including whether the mechanism is explained or only empirical;
- regression proof, including the recorded pre-fix failure;
- handoff route and any residual risk.

For every `partial` or `blocked` verdict, name the missing evidence, the impact, the next action, and the accountable owner when known.

Finish with the minimal reproduction command, the evidence paths, the regression test path, the recommended profile, and anything the investigation deliberately did not change.
