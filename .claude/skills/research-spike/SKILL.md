---
name: research-spike
description: Answer a technology or approach question with evidence before a plan commits to it. Surveys prior art inside the repository and outside it, compares real alternatives, confirms current versions and compatibility from primary sources rather than memory, and when the question cannot be settled by reading, runs a disposable spike against pre-declared kill criteria. Ends in an explicit verdict that feeds implementation-planning. Use when a request needs a new dependency, a new technology, an unfamiliar integration, or a choice between approaches nobody has evidence for. Do not use to write product code, to replace planning, or to research a decision the repository has already made and documented.
---

# Research Spike

Settle one question with evidence, then stop. Research produces a verdict and its sources; it never produces the implementation.

Spike code is disposable by construction. It exists to make a claim falsifiable and is deleted when the claim is settled, whichever way it lands.

Use the user's language unless the repository has an established documentation language. Keep observed reality separate from inference, proposals, and user decisions.

## Operating contract

- Frame one falsifiable question before gathering anything. A survey without a question expands until it runs out of budget.
- Track two independent axes: provenance (`Observed`, `User-confirmed`, `Inferred`, `Unknown`, `Contradiction`) and decision state (`N/A`, `Proposed`, `Undecided`, `Decided`, `Accepted risk`, `Out of scope`). A benchmark you ran is `Observed`; a vendor's claim about its own product is not.
- Never state a version, an API shape, a limit, or a compatibility fact from memory. Cite the primary source and the date you retrieved it. Model training data is not a source.
- Prefer the existing stack. The incumbent is an alternative in every comparison and wins ties, because adopting nothing has no migration cost and no new operational surface.
- Declare kill criteria before writing spike code. A spike that cannot fail is a demo.
- Spike code never becomes the implementation. It runs in a disposable worktree, is never merged, and is torn down at the end regardless of the verdict.
- Do not install dependencies, run generators, or touch shared state outside the disposable worktree without explicit authorization.
- Do not agree reflexively with the requested technology. If the evidence favours something else, including doing nothing, say so once with concrete consequences and let the operator decide.
- Research authorizes nothing. Its verdict is an input to `implementation-planning`, which is itself gated by plan approval.

Read each reference completely before the phase it governs:

- [references/spike-protocol.md](references/spike-protocol.md) before writing any spike code.
- [references/research-report.md](references/research-report.md) before writing the verdict artifact.

Read [examples/worked-example.md](examples/worked-example.md) when the expected shape of a finished report is unclear. It is illustrative, not evidence.

## State machine

```text
QUESTION_FRAMED
  -> PRIOR_ART_MAPPED
  -> OPTIONS_IDENTIFIED
  -> DESK_EVIDENCE_GATHERED
  -> SETTLED_BY_READING | SPIKE_REQUIRED
  -> SPIKE_SCOPED
  -> SPIKE_RUN
  -> SPIKE_TORN_DOWN
  -> VERDICT
  -> HANDOFF
```

If desk evidence settles the question, go straight to `VERDICT`; a spike that adds no decision value is waste. If the spike disproves the framing rather than the option, return to `QUESTION_FRAMED` instead of adjusting the criteria to fit the result.

## 1. Frame the question

State one question whose answer changes what gets built, in a form that can come out either way.

Record with it:

- the decision the answer unblocks, and who owns that decision;
- what the answer must be true of: the actual load, data shape, deployment target, and constraints of this project, not a generic benchmark;
- the budget: how much time and how much new operational surface this decision is worth;
- what would make research unnecessary, such as an existing ADR or an established project standard.

Reject the request as unnecessary when the repository has already decided and documented this, and say where.

## 2. Map prior art

Look inside before looking outside.

1. Search the repository for an existing solution to the same problem, an abandoned attempt, or an adjacent pattern the team already maintains. Read the tests that protect it.
2. Read the resolved context index, ADRs, and any prior research artifacts. A rejected alternative from last year is evidence, and so is the reason it was rejected.
3. Check the dependency manifests for something already installed that solves this. Adopting an existing dependency beats adding one.
4. Only then look outside: official documentation, the project's own issue tracker for known limitations, and independent reports of production use.

## 3. Identify real alternatives

Produce at least two credible options plus the incumbent, which is always one of them and may be "do nothing" or "extend what exists".

An option is credible only when it can actually satisfy the constraints from phase 1. Do not pad the comparison with options nobody would choose.

## 4. Gather desk evidence

For every option, establish from primary sources:

- the current stable release, its release date, and the release cadence;
- compatibility with this project's runtime, framework versions, deployment target, and existing ecosystem;
- the license and whether it is compatible with this project's constraints;
- the operational surface it adds: what must be run, monitored, upgraded, secured, and backed up;
- known limitations, open defects that matter here, and the maintenance signal;
- the migration and exit cost, including what it would take to remove it later.

Record every claim with its source URL and retrieval date. Mark anything you could not verify as `Unknown` rather than filling it in from plausibility.

When the project has no network access, use versions proven by the local manifests and lockfiles, and mark every consequential currency claim as requiring verification before adoption. Do not guess.

## 5. Decide whether a spike is needed

A spike is justified only when desk evidence leaves a decision-relevant question open and running code would close it.

Do not spike to learn a technology, to demonstrate enthusiasm, or to confirm something the documentation already states. State explicitly when the question is settled by reading and skip to the verdict.

## 6. Run the spike

Follow the spike protocol. In short: a disposable worktree, pre-declared kill criteria, the narrowest code that makes the question falsifiable, one variable at a time, evidence captured as it runs, and teardown at the end.

Report what the spike disproved as prominently as what it confirmed.

## 7. Reach a verdict

Give exactly one verdict:

- `adopt` — evidence supports it for this project and its constraints;
- `adopt-with-constraints` — supported only within named boundaries, each with the evidence that draws it;
- `reject` — the evidence favours an alternative or the incumbent; name which;
- `needs-more-evidence` — the question is still open; name the exact missing evidence and what would produce it.

Never return a verdict without naming the losing options and why they lost. A comparison with no losers was not a comparison.

## 8. Hand off

Write the research artifact using the report reference, then hand it to `implementation-planning` as evidence, not as a plan. Return a federation record with its path, content hash, source revision, and consumers so `project-context-initializer` can route to it.

When the verdict is `adopt` or `adopt-with-constraints` for a new technology, the planning workflow still owns version pinning, slicing, and the gate contract. Research supplies the version evidence; it does not commit the project to it.

## Non-interactive runs

In a non-interactive run, complete every phase that reading can complete and stop before installing anything or running a spike that needs authorization. Return the framed question, the option comparison, the desk evidence, and the exact authorization the spike requires. Do not manufacture a verdict the evidence does not support.

## Completion report

Report a separate verdict per dimension using `complete`, `partial`, `blocked`, or `not-applicable` with a reason:

- prior art, inside and outside the repository;
- option coverage, including the incumbent;
- primary-source evidence, including anything unverifiable;
- spike execution and teardown, or why no spike was needed;
- the verdict and its residual unknowns.

For every `partial` or `blocked` verdict, name the missing evidence, the impact, the next action, and the accountable owner when known.

Finish with the artifact path, the federation record, the disposable worktree's removal, and the exact decision that is now unblocked.
