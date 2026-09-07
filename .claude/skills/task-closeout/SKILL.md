---
name: task-closeout
description: Close the loop after delivered work by triaging what was actually learned and emitting it where it will be found again. Separates a durable decision that needs an ADR from a process rule that belongs in agent instructions, a project fact that belongs in durable agent memory, a narrative that belongs in the work log, and noise that belongs nowhere. Use after an implementation run, a bug fix, a research verdict, an incident, or any task whose lessons would otherwise stay in a transcript. Do not use to produce delivery evidence, to re-verify an implementation, to invent conclusions the work did not establish, or to publish outside the repository without explicit authorization.
---

# Task Closeout

Turn a finished piece of work into knowledge the next agent and the next person will actually encounter, then hand the new artifacts back to the project context so they are discoverable.

Closeout reads delivery evidence; it never rewrites it. A gate that failed, a target that was not reached, and an obligation that was deferred all stay exactly as the delivering skill recorded them.

Use the user's language unless the repository has an established documentation language. Keep observed reality separate from inference, proposals, and user decisions.

## Operating contract

- Emit only what the delivered work actually established. A closeout that invents a lesson is worse than no closeout, because it is durable.
- Apply the already-recorded test before every emission: if the code, its tests, the commit history, an existing ADR, or the current instruction files already carry the fact, do not restate it. Record only what a competent reader could not recover from those sources.
- Track two independent axes: provenance (`Observed`, `User-confirmed`, `Inferred`, `Unknown`, `Contradiction`) and decision state (`N/A`, `Proposed`, `Undecided`, `Decided`, `Accepted risk`, `Out of scope`). A lesson drawn from one run is `Inferred` until evidence or the operator confirms it.
- Give every candidate exactly one disposition: `emit-adr`, `emit-instruction-rule`, `emit-agent-memory`, `emit-work-log`, or `discard-as-noise`. Do not emit the same content to several targets and call it thorough.
- Never write a secret, credential, token, customer record, or personal machine path into a durable artifact. Record the key name and where it is defined instead of its value.
- Publishing to a system outside the repository is an outward-facing side effect. Draft it always; send it only under explicit authorization.
- Do not stage, commit, push, or merge unless the user separately asks. Closeout writes files and reports paths.
- Do not agree reflexively that the run went well. If the evidence shows a gate that was deferred and never closed, a target below the selected terminal outcome, or a workaround that will bite later, that is the most valuable thing to record.

Read each reference completely before the phase it governs:

- [references/lesson-triage.md](references/lesson-triage.md) before assigning any disposition.
- [references/knowledge-targets.md](references/knowledge-targets.md) before writing or sending any artifact.

Read [examples/worked-example.md](examples/worked-example.md) when the expected shape of a finished closeout is unclear. It is illustrative, not evidence.

## State machine

```text
INTAKE
  -> EVIDENCE_GATHERED
  -> TRIAGED
  -> DRAFTED
  -> AUTHORIZED
  -> EMITTED
  -> FEDERATED
  -> REPORTED
```

If triage exposes that the delivered work is materially misdescribed by its own evidence, return to `EVIDENCE_GATHERED` rather than writing the comfortable version. If authorization for an external target is unavailable, emit every repository-local target, keep the external artifact as a draft, and report it as `partial`.

## 1. Intake and evidence

Establish what was delivered before deciding what was learned.

1. Identify the work: the request, the plan when one exists, the branch, the commits, the PR, and the selected terminal outcome when the run came from `implementation-orchestrator`. Without version control, identify it by the request, the artifact paths written, and their content hashes, so the log still points at something verifiable.
2. Read the durable run evidence when present, starting from the run router under `context/implementation-runs/<run-id>/` and its event log. Read the actual gate results, not the summary.
3. Read the conversation for decisions the repository never recorded: rejected alternatives, constraints the operator stated, and assumptions that turned out wrong.
4. Read the current instruction files and the resolved context index so the already-recorded test has something to test against.
5. Separate three kinds of source: what the work proved, what the operator decided, and what an agent inferred while working. Only the first two can become a durable claim without a marker.

If no durable evidence exists because the work was informal, say so and continue from the conversation alone. Closeout degrades to a smaller emission set; it does not fabricate a run ledger.

## 2. Triage what was learned

Follow the triage reference. Build one candidate ledger before writing anything:

| Candidate | Provenance | Disposition | Why this target | Evidence |
| --- | --- | --- | --- | --- |

Rules that decide a disposition:

- A choice between real alternatives that constrains future work is `emit-adr`. It must name the alternatives that lost and why.
- A correction to how agents should work, stated by the operator or proven by a failure, is `emit-instruction-rule`. It belongs in the instruction files, not in a memory note nobody loads.
- A durable project fact that the repository does not encode, such as an environment constraint or an external system's real behavior, is `emit-agent-memory`.
- A narrative of what happened, useful for tracing but not for deciding, is `emit-work-log`.
- Anything the already-recorded test rejects, anything true only of this transcript, and anything an agent merely felt is `discard-as-noise`. Discarding is a normal outcome and must appear in the report.

A single run usually produces zero to two durable emissions. A closeout that emits a dozen lessons has stopped filtering.

## 3. Draft the emissions

Write each artifact in the form its target expects, using the templates in the knowledge-targets reference.

Every durable artifact records what it claims, the evidence behind the claim, and the condition that would invalidate it. An ADR without rejected alternatives, a rule without its reason, or a memory note without its expiry condition will be obeyed long after it stops being true.

Convert relative time to absolute dates. "Last week" is unreadable in six months.

## 4. Authorize and emit

Repository-local writes proceed under the authority that invoked this skill: ADR files, instruction-file managed blocks, work-log files, and durable agent memory.

Anything that leaves the repository, including an external knowledge base, an issue tracker, or a chat surface, requires explicit authorization naming the destination. Present the exact content and destination, then wait. Never send a draft to prove what it would look like.

Preserve everything outside a managed block. When an instruction file already carries a rule that the new one contradicts, do not append a second rule; reconcile them and show the operator the replacement.

## 5. Federate into project context

New durable artifacts are invisible until the context index routes to them.

1. Return a reconciliation record for each emitted repository artifact: path, purpose, owner, source revision, content hash, and which downstream skill should read it.
2. Do not edit an index owned by `project-context-initializer` directly. Hand it the record, or run its refresh when a broader remap is warranted.
3. When an ADR changes an architecture claim that the existing context asserts, name the contradiction explicitly so the refresh resolves it instead of stacking both versions.

## Non-interactive runs

In a non-interactive run, complete the full triage, write every repository-local artifact, and stop before any external destination. Return the drafts and the exact authorization needed. Do not manufacture the operator's approval and do not silently drop the external target.

## Completion report

Report a separate verdict per dimension using `complete`, `partial`, `blocked`, or `not-applicable` with a reason:

- evidence intake, including anything unreadable;
- triage, including the candidates discarded and why;
- repository-local emissions, with exact paths;
- external emissions, with destination and authorization state;
- context federation records returned.

For every `partial` or `blocked` verdict, name the missing evidence, the impact, the next action, and the accountable owner when known.

Finish with the artifact paths, the reconciliation records, anything explicitly discarded, and any unresolved contradiction between the new knowledge and what the repository currently asserts.
