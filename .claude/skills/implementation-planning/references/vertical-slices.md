# Vertical Slice Guide

Read this guide when converting an agreed solution into implementation work.

## Definition

A vertical slice delivers one observable capability through every layer required for that capability. It is organized around behavior, not architecture.

A valid slice answers all of these:

- What can the actor do after this slice that they could not do before?
- Which happy-path step and which edge cases does it own?
- Which interfaces, logic, data, configuration, documentation, and observability are the minimum needed?
- Can a fresh executor implement it without relying on an unfinished sibling slice, using a subagent when available?
- Can the completed capability be run and verified through the real product surface, with any preparatory rollout stages verified operationally as they ship?
- Can a reviewer accept or reject it as one coherent change?

If any answer is missing, redraw the boundary.

## Invalid horizontal decomposition

Avoid plans like:

1. Create database schema.
2. Implement backend services.
3. Add API endpoints.
4. Build frontend screens.
5. Add tests.

None of these alone proves a working user outcome. They create long-lived integration risk and force later agents to infer contracts.

Instead, decompose by capability. For example:

1. A signed-in user can create the smallest valid record and see it in the UI.
2. The user sees actionable validation for invalid input and can correct it.
3. The user can resume or safely retry after the highest-risk interruption.

Each slice contains only the schema, service, endpoint, UI, tests, and documentation needed for that outcome.

## Decomposition procedure

1. List the actor's journey as observable steps.
2. Identify the smallest walking skeleton that traverses the real interface, logic, and persistence path.
3. Group edge cases by the capability whose behavior they alter. Do not postpone all failures to a final hardening slice.
4. Fold setup, migrations, flags, telemetry, and documentation into the first slice that consumes them.
5. Split a slice when it contains multiple actor outcomes, cannot fit one review cycle, or needs unrelated change surfaces.
6. Merge slices when neither produces a coherent runnable state alone.
7. Build the dependency graph from actual contracts, not an assumed layer order.
8. Check file, schema, state, fixture, and generated-artifact ownership before claiming parallel safety.
9. Check runtime resource ownership against the orchestrator's [resource isolation contract](../../implementation-orchestrator/references/resource-isolation.md). Isolate every applicable resource or serialize the slices.

## Capability slices and delivery stages

A capability slice is the unit of user value and E2E acceptance. A delivery stage is an ordered rollout step inside that slice.

Execution profile does not change slice boundaries or verification coverage. A later `quick-dev` or `quick-bug-fix` decision may run only focused tests and cheap checks per slice while marking Sonar, independent review and real-surface E2E `DEFERRED_TO_FINAL`. The plan must still define those gates precisely, and the assembled feature must close them before remote delivery. Never redesign a horizontal or oversized slice merely to qualify for quick cadence.

For a compatibility-sensitive change, one capability may require:

1. **Expand:** Add backward-compatible storage or contracts without changing behavior.
2. **Backfill:** Populate or reconcile data while old and new readers remain valid.
3. **Cut over:** Enable the actor-visible behavior and run the capability E2E.
4. **Contract:** Remove compatibility paths only after evidence establishes they are unused.

Do not call these four separate vertical slices. Make each a separately reviewable stage packet with its own implementation scope, operational consumer, semantic invariant, tests, verification, rollout condition, rollback, and required authorization. The completed capability slice still owns the user-visible outcome and real-surface E2E. The plan itself authorizes none of the operational stages.

## Slice packet

Give every implementing subagent a self-contained packet with these fields:

### Identity

- **ID and capability:** One sentence in the form "The actor can ...".
- **Why now:** The requirement or risk this slice resolves.
- **Status:** Pending, active, blocked, or complete.

### Boundaries

- **In scope:** Exact behaviors and named edge cases.
- **Out of scope:** Adjacent behavior this slice must not add.
- **Global constraints:** Project rules and invariants that apply unchanged.
- **Likely change surface:** Existing and new files, modules, schemas, routes, or components. Use precise paths when reconnaissance establishes them.
- **Conflict footprint:** Files, migrations, generated artifacts, shared fixtures, or state that make concurrent editing unsafe.
- **Worktree resource lease:** The resources this slice needs, taken from the canonical isolation contract, each marked unique or serialized.

### Contracts and dependencies

- **Prerequisites:** Merged slices or external conditions required before work starts.
- **Consumes:** Existing interfaces and exact invariants the slice relies on.
- **Produces:** Public behavior or contracts later slices may rely on.
- **Parallel-safe with:** Only slices proven not to share dependencies or conflict footprints.

### Behavior

- **Happy path:** Numbered actor actions and visible results.
- **Edge cases and recovery:** Inputs or failures this slice owns and the required response.
- **Acceptance criteria:** Binary, observable statements.

### Verification

- **Focused tests:** Unit, component, contract, or integration checks tied to named risks.
- **Expected initial RED:** The acceptance assertion that must fail because behavior is missing, plus how harness/environment failure is excluded.
- **Static analysis:** Exact repository-defined command and working directory.
- **Sonar:** Scanner and actual Quality Gate lookup, a named CI-only deferral, or evidence that Sonar is not configured.
- **Independent review:** Slice-specific regression, logic, edge-case, security/data/concurrency, architecture/ADR, and test-strength risks.
- **Verification verdict:** Real-surface E2E, consumer-boundary E2E, strongest non-E2E evidence, or blocked.
- **E2E setup:** Environment, seed data, roles, accounts, and cleanup.
- **E2E flow:** Exact user actions through the real interface.
- **Expected evidence:** Visible result, persisted state, logs, screenshots, or other auditable proof.
- **Deliberate-break check:** When proportionate, describe a controlled way to prove the test fails if the protected behavior is broken.
- **Commands:** Exact focused and E2E commands confirmed in the project, plus any precisely defined command this slice will introduce.
- **Post-merge integration check:** Exact feature-branch command and cross-slice behavior to verify immediately after integration.
- **Quick-cadence disposition:** Which gates, if any, may be `DEFERRED_TO_FINAL` when the approved execution profile is quick; include their exact final scope and command. Omit when quick eligibility is not proven.
- **Exception:** For strongest non-E2E evidence, state why E2E is inaccessible, residual risk, accountable owner, expiry or closing condition, and the plan to add the missing proof.

### Delivery safety

- **Observability:** Logs, metrics, audit trail, or user feedback needed to detect failure.
- **Rollout:** Flag, migration order, compatibility window, or deployment constraint.
- **Rollback or fallback:** How to return to a safe state.
- **Delivery stages:** If required, ordered expand, backfill, cutover, and contract packets with per-stage scope, tests, invariants, proof, rollback, and authorization gate.

## User-surface E2E hierarchy

Choose the highest applicable surface:

| Product | Primary E2E | Request-level fallback |
| --- | --- | --- |
| Web UI | Real browser navigation, clicks, typing, visible assertions; headed locally by default | Setup only; never replace the UI journey |
| Mobile UI | Simulator or device gestures and visible state; visible/headed locally by default | Setup only when the app exposes no safe fixture path |
| Desktop UI | Launch the app and drive controls or accessibility actions visibly/headed by default | Setup only |
| CLI/TUI | Spawn the real executable; use args, stdin or keys; assert output, exit code, and effects | Not applicable; the process is the user surface |
| API-only service | Real public API or event consumer flow | This is the product surface; explain the boundary |
| Library | Consumer-level integration or executable example | Direct internal unit calls are supporting tests only |

Mock only external systems that are unsafe, costly, irreversible, or nondeterministic. Keep internal application boundaries real. Record why a mock is necessary and what production contract it represents.

For local interactive E2E, use headed/visible execution unless repository instructions or the available environment require otherwise. Record the exception and retained evidence; headless success alone does not satisfy a user request for headed verification.

For an interactive product without a usable harness, choose explicitly:

1. add the smallest harness in the first capability that needs it;
2. use the strongest non-E2E evidence under an owned, time-bounded exception;
3. mark the capability blocked.

Do not claim that request-level checks are UI E2E.

## Independence versus parallelism

Every slice must be independently understandable and executable after its declared prerequisites. Parallel execution is a stricter claim.

Mark two slices parallel-safe only if all are true:

- neither consumes a contract the other still needs to create;
- they do not edit the same high-churn files or generated outputs;
- they do not create competing migrations or mutate the same shared fixture/state;
- their E2E environments can run without shared-state collisions;
- every applicable resource in the canonical isolation contract can be isolated for each slice, or provably does not overlap;
- merging either first does not invalidate the other's assumptions.

If not, sequence them. Independence of the brief is still valuable even when implementation is sequential.

## Final verticality test

Reject a capability slice if its title could be replaced by a layer name, if its E2E must wait for a later capability slice, or if the actor cannot observe its completed outcome. Non-visible delivery stages are permitted only inside that capability, with operational proof and rollback. Attach other enabling work to the first observable capability that needs it.
