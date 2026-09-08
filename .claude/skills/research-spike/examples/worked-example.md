# Worked example: queue for invoice export

> Illustrative only. The product, versions, measurements and sources in this
> example are invented to show the shape of a completed research artifact. Never
> reuse a number or a version from this file as evidence.

## Question

Should invoice export move from the existing scheduled task to a dedicated job queue?

- **Decision unblocked:** the export slice of the billing plan cannot be sliced until the execution model is fixed.
- **Decision owner:** billing module owner.
- **Success criterion declared up front:** a queue keeps p99 export latency under 5 minutes at 20x current volume without a second operational component to run.
- **Kill criterion declared up front:** if the existing scheduled task reaches the same target after the batching change already planned, stop and reject.
- **Budget:** one day, abandoned at that point regardless of progress.

## Constraints this answer must satisfy

Deployment target runs one application container with no worker tier today (`deploy/compose.yaml`). The team is two people; a component that needs its own monitoring and upgrade path is a real cost, not a rounding error. Exports must be idempotent because customers retry them (`src/billing/export.ts:88`).

## Prior art

| Source | What it establishes | Provenance |
| --- | --- | --- |
| `src/jobs/scheduler.ts` | A scheduled-task runner already exists and handles retries | Observed |
| `docs/adr/0007-no-worker-tier.md` | A worker tier was rejected two years ago on operational cost, before volume grew | Observed |
| Conversation with the module owner | Volume is expected to grow 20x within a year | User-confirmed |

The prior rejection is not binding, because the constraint it rested on has changed. It does set the bar: the new evidence must show the operational cost is now worth paying.

## Options

| Option | Version | License | Operational surface added | Exit cost | Source and retrieval date |
| --- | --- | --- | --- | --- | --- |
| Extend the existing scheduler (incumbent) | n/a | n/a | none | none | `src/jobs/scheduler.ts` |
| Dedicated queue service | 4.2.0, released 2026-05-14 | Apache-2.0 | broker to run, monitor, upgrade, back up | high; job semantics leak into call sites | vendor docs, retrieved 2026-08-20 |
| Database-backed queue library | 2.1.3, released 2026-07-02 | MIT | none beyond existing database | low; table plus a worker loop | library docs, retrieved 2026-08-20 |

### Trade-offs

The dedicated broker is the only option that survives losing the database, which nothing in the requirements asks for. The library option keeps one datastore and one deployment unit, at the cost of queue throughput bounded by database write capacity — irrelevant at the volumes in question, decisive at 100x.

## Evidence

| Claim | Source | Retrieved | Provenance |
| --- | --- | --- | --- |
| Library 2.1.3 is current stable | library releases page | 2026-08-20 | Observed |
| Library requires the database version already deployed | library docs, compatibility page | 2026-08-20 | Observed |
| Current export p99 is 11 minutes | `metrics/export-latency`, 30-day window | 2026-08-20 | Observed |
| 20x volume projection | module owner | 2026-08-20 | User-confirmed |
| Broker upgrade cadence | vendor docs | 2026-08-20 | Unknown; the page does not state a policy |

## Spike

- **Why reading was insufficient:** whether the planned batching change alone reaches the target was unknown, and it decides whether any queue is needed.
- **Kill criteria declared up front:** batched scheduler reaches p99 under 5 minutes at 20x synthetic volume.
- **Environment:** disposable worktree, leased database schema and port, 20x synthetic export set generated from a production-shaped fixture.
- **Method:** ran the current scheduler and a batched variant against the same dataset, five runs each, cold start excluded, everything else held constant.
- **Result:** current scheduler p99 41 minutes (range 38–46). Batched variant p99 4 minutes 20 seconds (range 3:55–4:48).
- **Disproved:** the assumption that batching would be insufficient. That assumption was the entire reason a queue was proposed.
- **Teardown:** worktree removed, schema and port released, working checkout unchanged.

## Verdict

**Verdict:** reject

Batching the existing scheduler meets the declared target at 20x volume with no new operational component, which triggers the kill criterion recorded before the spike began. Adding a queue now would buy headroom nobody has asked for and a component two people would have to operate.

### Why the alternatives lost

| Option | Why it lost |
| --- | --- |
| Dedicated queue service | Solves a durability requirement that does not exist, at the highest operational cost of the three |
| Database-backed queue library | Credible and cheap, but unnecessary once batching meets the target; revisit if the target moves |

### Residual unknowns

| Unknown | Impact if wrong | How it would be closed | Owner |
| --- | --- | --- | --- |
| Behavior above 20x | Batching may stop scaling and the decision reopens | Re-run the same harness at 100x | Billing module owner |
| Synthetic data shape vs production | Measured p99 may be optimistic | Replay a production-shaped export week | Billing module owner |

## Handoff

- **Consumer:** implementation-planning, for the export slice
- **What planning still owns:** batch size, backpressure behavior, rollout and rollback
- **Revisit when:** measured p99 exceeds 4 minutes at production volume, or a durability requirement appears
- **This artifact authorizes nothing.**
