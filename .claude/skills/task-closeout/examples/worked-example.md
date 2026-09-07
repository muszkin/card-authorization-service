# Worked example: closeout after the export batching change

> Illustrative only. The paths, decisions and artifacts in this example are
> invented to show the shape of a completed closeout. Never reuse a conclusion
> from this file as evidence about a real repository.

## Evidence intake

Run `2026-08-21T0900Z-export-batching`, profile `standard`, terminal outcome `staging-green`, reached. Plan `docs/plans/2026-08-18-export-batching.md`. Three slices, one repair after a red integration check. Research verdict `reject` for the queue option is linked from the plan.

Read: the run event log, the actual gate results, the plan's accepted risks, and the conversation where the module owner set the latency target.

One gate did not close cleanly: Sonar was `DEFERRED_TO_CI` and passed on the PR, which is correct, but the run log shows it was described as green locally in an interim report. Recorded as a candidate.

## Triage ledger

| Candidate | Provenance | Disposition | Why this target | Evidence |
| --- | --- | --- | --- | --- |
| Batching the scheduler was chosen over a queue, with the earlier no-worker-tier ADR reconsidered | Observed | `emit-adr` | A choice between real alternatives that constrains the next export change; the losing options and the measurement belong in the record | research verdict, plan decision ledger |
| The 5-minute p99 target came from the module owner, not from a requirement document | User-confirmed | `emit-agent-memory` | Nothing in the repository records where the number came from, and the next change will be measured against it | conversation, 2026-08-18 |
| Shard split on 2026-08-08 changed which tests share fixtures | Observed | `emit-adr` candidate, downgraded to `discard-as-noise` | The commit and its message already record it; a reader hitting this will find it | `git log ci/shards.yaml` |
| Batch size 500 chosen | Observed | `discard-as-noise` | The constant and its comment are in the code; an ADR would restate it | `src/billing/export.ts:112` |
| Interim report called a `DEFERRED_TO_CI` gate green | Observed | `emit-instruction-rule` | A process failure that will repeat, and the run contract already forbids it; the rule needs to be where reports are written | run event log |
| The team used the existing test harness | Observed | `discard-as-noise` | Derivable from the repository | n/a |

Two durable emissions, one rule, three discarded. The shard-split candidate was the closest call; the already-recorded test rejected it because the commit message states the change and its reason.

## Emissions

### `emit-adr`

`docs/adr/0014-batch-export-instead-of-queue.md`, status accepted, superseding nothing but explicitly reconsidering `0007-no-worker-tier.md` and linking both directions. Records the measured p99 for both options, the 20x volume assumption, and the revisit condition: measured p99 above 4 minutes at production volume, or a durability requirement appearing.

### `emit-instruction-rule`

Added outside every managed block in the repository instruction file:

```markdown
- Never describe a `DEFERRED_TO_CI` or `DEFERRED_TO_FINAL` obligation as green in any interim report. It is an open obligation until the named check passes on the current head, and reporting it as passed hides the only signal that it is still open.
```

Checked first for an existing rule covering it: the run contract forbids the behavior, but no instruction file did, which is why the interim report got it wrong.

### `emit-work-log`

`context/implementation-runs/2026-08-21T0900Z-export-batching/closeout.md`, recording the identifiers, the exact gate results including the deferral, one open obligation, and the discarded candidates so the filter is auditable.

**Open obligations carried forward:**

| Obligation | Owner | Trigger to close |
| --- | --- | --- |
| Other tests may call production deletion paths directly | Billing module owner | Next change touching the billing test suite |

## Federation records

| Field | Value |
| --- | --- |
| path | `docs/adr/0014-batch-export-instead-of-queue.md` |
| purpose | Why export batches instead of queueing, with the measurement and revisit condition |
| owner | task-closeout |
| producer run | 2026-08-21T0900Z-export-batching |
| source revision | `4d1e77c` |
| content hash | `sha256:…` |
| consumers | implementation-planning, research-spike |
| contradicts | `docs/adr/0007-no-worker-tier.md` is now partially superseded; the context map still asserts no worker tier is under consideration |

The contradiction is declared rather than resolved here, so the context refresh reconciles it instead of stacking both claims.

## Report

- evidence intake: `complete`
- triage: `complete`, three candidates discarded with reasons
- repository-local emissions: `complete`, three artifacts
- external emissions: `not-applicable`, no external destination was requested
- context federation: `complete`, one record with a declared contradiction
