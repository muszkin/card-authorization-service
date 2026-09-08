---
name: implementation-orchestrator
description: Execute an approved implementation-planning artifact through one coordinating orchestrator and isolated worktree subagents. Select quick-dev, quick-bug-fix, standard, or bug execution; default to multi-agent fire-and-forget delivery with an explicit terminal outcome; and persist through SHA-bound test, static, Sonar, independent-review, E2E, CI, staging, or authorized production repair loops. Use for planned feature or bug implementation. Do not use to invent requirements or replace implementation-planning.
---

# Implementation Orchestrator

Turn an approved plan into a verified delivery without converting failed gates into stop points or waived work.

The main agent is the orchestrator. Product-code implementation belongs to slice workers in separate Git worktrees. Independent reviewers do not inherit the worker's conclusions and do not edit code.

## Required inputs

Before changing product code, obtain all of the following:

1. The exact approved plan produced by `implementation-planning`, including its path and content hash.
2. The trusted instruction that authorizes implementing that plan. A plan file saying `approved` is an audit record, not authorization by itself.
3. Durable project context produced by `project-context-initializer`:
   - the resolved context index (default `context/map/INDEX.md`) and the manifest or inventory it routes to,
   - overview, architecture, dependencies, flows and delivery/runtime findings,
   - ADRs, decision records and project documentation,
   - the nearest `.agents/project-context.md` for every planned change surface,
   - linked prior plans, research, reviews, implementation runs or release evidence relevant to the work.
4. The repository's actual branch, CI, staging and production topology.
5. A confirmed execution contract: profile, orchestration mode, terminal outcome, model policy and automatic-escalation permission.

If project context is missing or materially stale, run or refresh `project-context-initializer` before implementation. If the plan is missing, unapproved or materially contradicted by current evidence, return to `implementation-planning`; do not silently invent a replacement plan.

Read [intake-and-authorization.md](references/intake-and-authorization.md) completely during intake.
Read [execution-profiles.md](references/execution-profiles.md) completely before recommending or confirming how the run will execute.

## Non-negotiable invariants

- Use one orchestrator for the run. Only it owns scheduling, the central ledger, integration, remote PR state and environment promotion.
- Do not let the orchestrator implement product code. Delegate even when the run contains one slice; use a different agent for independent review.
- The default orchestration mode is bounded multi-agent `fire-and-forget`; `supervised` still uses slice workers and isolated worktrees but pauses at recorded checkpoints.
- Immediately before the first implementation side effect, ask for or restate the operator-confirmed profile, orchestration mode, terminal outcome, model policy and automatic-escalation rule. Do not duplicate a still-current answer already captured by `implementation-planning`.
- Recommend a mode from evidence and challenge an unsafe, wasteful or mismatched choice once with concrete consequences. The operator has the final word inside the authorization envelope and repository/safety controls.
- Give every implementation or repair assignment its own branch and worktree based on the intended parent SHA.
- Parallelize only slices whose dependency edges, change surfaces and runtime resources are all proven independent.
- Bind every passing gate to an immutable commit SHA, exact command and evidence location. A later code change makes downstream gates stale.
- Treat RED static analysis, Sonar, review, E2E or CI as repair work, not a terminal outcome.
- Do not replace real user-surface E2E with direct HTTP requests when a browser, desktop UI, mobile UI or terminal interaction is available.
- Never call a skipped, unavailable or deferred gate green.
- Quick profiles change gate cadence, never the meaning of a gate or the final quality bar. `DEFERRED_TO_FINAL` is an open obligation, not a pass; every full final gate must close on the assembled feature SHA.
- Never bypass protected branches, required reviews, required CI or repository release policy.
- Do not use destructive cleanup, broad staging such as `git add .` or `git add -A`, or overwrite unrelated user changes.
- Continue only to the confirmed terminal outcome. Production merge/deployment is allowed only when `production-green` is explicitly selected and the authorization, rollback, artifact-provenance, production verification and observation contract are complete.

## Load supporting instructions progressively

Read each reference completely before the stage it governs:

- [worktree-and-delegation.md](references/worktree-and-delegation.md) before creating worktrees or delegating slices.
- [resource-isolation.md](references/resource-isolation.md) before allocating any runtime resource or scheduling a parallel cohort.
- [run-artifacts.md](references/run-artifacts.md) before initializing or resuming the run ledger.
- [execution-profiles.md](references/execution-profiles.md) before selecting cadence, orchestration, terminal outcome or worker models.
- [slice-quality-loop.md](references/slice-quality-loop.md) and [quality-gates.md](references/quality-gates.md) before any slice implementation.
- [review-rubric.md](references/review-rubric.md) before dispatching an independent reviewer.
- [integration-ci-staging.md](references/integration-ci-staging.md) before integrating the first slice or making remote changes.

## Execution state machine

Use this state machine; do not compress it into a checklist:

```text
INTAKE
  -> AUTHORIZED
  -> CONTEXT_READY
  -> TOPOLOGY_READY
  -> EXECUTION_CONTRACT_SELECTED
  -> BASELINE_RECORDED
  -> SLICES_RUNNING
  -> FEATURE_LOCAL_GREEN
  -> TARGET_REACHED                         # local-green
  |  FEATURE_READY_FOR_PR
  -> INTEGRATION_PR_GREEN
  -> TARGET_REACHED                         # ready-pr
  |  INTEGRATION_MERGED
  -> TARGET_REACHED                         # integration-merged
  |  STAGING_DEPLOYED
  -> STAGING_REVISION_VERIFIED
  -> STAGING_GREEN
  -> TARGET_REACHED                         # staging-green
  |  PRODUCTION_PR_OPEN
  -> TARGET_REACHED                         # production-pr-ready
  |  PRODUCTION_DEPLOYED
  -> PRODUCTION_GREEN
  -> TARGET_REACHED                         # production-green
```

In `standard` and `bug`, each slice follows:

```text
PLANNED
  -> WORKTREE_READY
  -> CONTEXT_LOADED
  -> BASELINE_PROVEN
  -> ACCEPTANCE_RED_PROVEN
  -> IMPLEMENTED
  -> STATIC_GREEN
  -> SONAR_ACCOUNTED
  -> REVIEW_GREEN
  -> E2E_GREEN
  -> READY_FOR_INTEGRATION
  -> INTEGRATED
  -> FEATURE_HEAD_GREEN
```

In `quick-dev` and `quick-bug-fix`, locally runnable heavy slice gates may use `DEFERRED_TO_FINAL` after focused tests and cheap build/type/static checks pass. The slice is only `QUICK_READY_FOR_INTEGRATION`, never fully green; the assembled feature must run the complete final chain.

Only an external or authority boundary may use `BLOCKED_EXTERNAL`. A normal red implementation gate stays active in the repair loop.

## Phase 1: freeze the execution contract

1. Resolve the plan path, approved revision and SHA-256 hash.
2. Record the user's authorization envelope and its exclusions.
3. Read context in this order:
   - root routing files,
   - architecture and ADRs,
   - flows and dependencies,
   - delivery/runtime/testing documentation,
   - nearest directory context for every slice,
   - related durable artifacts already linked by the context index.
4. Verify every command named by the plan against current repository scripts and documentation.
5. Detect the real default, integration, staging-trigger and production-deployment branches. Do not assume `main` or `develop`.
6. Detect required PR checks, Sonar mode, deployment mechanism and staging verification surface.
7. Reconcile the plan's slice DAG, file ownership and environment leases with current repository state.
8. Recommend an execution profile and terminal outcome from scope/risk evidence. Ask one compact pre-implementation question covering profile, `fire-and-forget` or `supervised`, terminal outcome, model policy and automatic escalation. If a current trusted answer came from planning, restate it and allow correction instead of asking twice.
9. Resolve worker and reviewer models through the resolution procedure in `execution-profiles.md`, and record the requested identifier, the resolved identifier and the reasoning effort. Do not carry a model name from this document into a dispatch without resolving it against the current host.
10. Create the durable run directory and baseline record described in `run-artifacts.md`.

Do not begin a slice while a material architecture rule, migration order, public contract or release boundary remains unresolved.

## Phase 2: create integration and slice worktrees

Create a dedicated feature-integration branch and worktree from the approved base SHA. Keep it separate from a dirty user checkout.

For each ready slice:

1. Create a slice branch and worktree from the recorded parent SHA.
2. Allocate exclusive runtime resources from the canonical list in [resource-isolation.md](references/resource-isolation.md).
3. Write a self-contained slice packet containing only the necessary plan excerpt, contracts, context paths, commands, acceptance scenarios, ownership and prohibitions.
4. Include selected profile, gate cadence, model assignment and escalation triggers in the packet.
5. Assign one worker as logical owner. The worker may not merge, push, deploy, change central run state or spawn its own reviewer.

The orchestrator may schedule a parallel cohort only when the proof in `worktree-and-delegation.md` passes.

## Phase 3: run every slice to verified local readiness

Apply the selected profile from `execution-profiles.md` and `slice-quality-loop.md`.

For `standard` and `bug`, the worker executes the complete slice loop:

1. Prove the existing application and test harness are healthy enough to test the slice.
2. Add or identify the acceptance E2E test and capture the expected RED caused by the missing behavior.
3. Implement the smallest complete vertical behavior.
4. Run focused tests and build/type checks relevant to the slice.
5. Stage only named intentional files and create a private candidate commit using repository commit conventions. This creates the immutable SHA to inspect; it is not permission to integrate.
6. Run repository-defined static analysis. RED returns to implementation.
7. Run Sonar and read the actual Quality Gate. RED returns to implementation. If the repository's only supported Sonar execution is a required CI check, record `DEFERRED_TO_CI` as an open obligation rather than calling it green.
8. Hand the exact `BASE...HEAD` diff to an independent read-only reviewer. Any material finding returns to implementation.
9. Run the real-surface E2E scenario. RED returns to implementation.
10. Mark the candidate ready only when all locally runnable required gates pass for the same SHA and every CI-only obligation is explicitly registered.

For `quick-dev` and `quick-bug-fix`, require the profile-specific pre-edit proof, focused regression/acceptance tests and cheap repository build/type/static checks. Register per-slice Sonar, independent review and real-surface E2E as `DEFERRED_TO_FINAL` only when the profile permits it. Do not describe the slice as gate-green.

`quick-bug-fix` and `bug` may not edit product code until the defect is reproduced at a meaningful boundary and the evidence distinguishes a real defect from harness or environment failure.

Every repair produces a new private candidate commit, invalidates the repaired gate and all gates after it, and re-runs them in order. Do not reuse a review verdict or E2E result from an older SHA.

When a failure repeats, change the diagnostic strategy: gather fresh evidence, trace from the boundary, form a falsifiable hypothesis and make a minimal fix. Do not repeat the same patch or waive the gate.

## Phase 4: integrate one slice at a time

The orchestrator acquires the integration lock and may perform a conflict-free mechanical update of the slice against current feature HEAD by rebase or merge according to repository policy. Any semantic conflict resolution, generated-output change or product/test content edit returns to the logical owner in the slice worktree and invalidates affected gates. After revalidation, the orchestrator performs a separate explicit integration into the feature worktree using the repository's required squash merge, merge commit, fast-forward or cherry-pick strategy. Updating a slice branch is not integration.

Immediately after each slice integration:

1. Run the feature-branch integration test set named by the plan.
2. Run cross-slice contract checks affected by the merge.
3. If RED, classify ownership and return a repair packet to the same logical owner.
4. Create that repair from the current feature HEAD in a fresh repair worktree.
5. Integrate the repair only after the active profile's required per-slice gates pass and every legitimate `DEFERRED_TO_FINAL` or `DEFERRED_TO_CI` obligation is registered without being called green. Escalate a quick profile if the repair no longer meets quick eligibility.

Do not integrate the next dependent slice while feature HEAD is red.

## Phase 5: verify the assembled feature

After all slices are integrated, first reconcile architecture/ADR/directory context and useful implementation evidence. If reconciliation changes tracked files, commit those changes as part of the feature candidate. Then freeze one immutable feature SHA and, for every profile without exception, run fresh combined gates:

1. full build, type checks, lint and static analysis;
2. full Sonar analysis and actual Quality Gate, or `DEFERRED_TO_CI` only when required CI is the repository's sole supported Sonar execution path;
3. fresh independent combined review of `merge-base(integration-target, feature-head)...feature-head`;
4. full real-surface E2E suite, headed locally when the repository supports it.

Any failure creates a repair assignment from current feature HEAD and restarts the invalidated gate chain. Any later tracked change makes affected evidence stale and must pass the chain again.

## Phase 6: deliver to the selected terminal outcome

After the complete local final chain, record `FEATURE_LOCAL_GREEN`. If the selected outcome is `local-green`, stop without pushing.

For higher outcomes, execute only the required prefix of this delivery sequence:

1. Push only the verified feature SHA and open a ready PR to the repository's actual integration target.
2. Wait for all required checks on the current PR head SHA. If CI is red, obtain logs, reproduce locally where possible, delegate a repair, re-run the full affected chain, push, and wait again. A green ready PR proves `ready-pr`.
3. Merge only when the authorization envelope includes the integration merge and all repository protections are satisfied. The verified merge SHA proves `integration-merged`.
4. When a higher outcome requires staging, verify the exact merged SHA or built artifact actually reached it; a successful deploy job alone is insufficient proof. A revision/digest mismatch enters its own provenance and redeploy/pipeline-repair loop and forbids acceptance E2E.
5. Only after `STAGING_REVISION_VERIFIED`, run staging E2E through the real user surface. If red, route a repair through the repository's normal PR/CI path, redeploy and repeat. Exact revision plus green E2E proves `staging-green`.
6. When a higher outcome requires a production handoff, open the production-target PR with exact evidence and remaining risks when the repository topology uses one. This proves `production-pr-ready`.
7. For `production-green`, verify explicit production authority again at the action boundary, merge/promote through normal controls, prove the expected SHA/artifact is live, run the authorized safe production smoke/E2E, observe the agreed health signals for the recorded window, and repair or roll back according to the pre-recorded rule until production is green or a true external/authority blocker exists.

Stop as soon as the selected terminal outcome is proved. Do not perform later release actions merely because they are technically available.

## Resume behavior

On resume, trust repository state and SHA-bound evidence, not old checkmarks:

1. Load the run manifest and append-only event log, then acquire or safely take over the exclusive orchestrator lease.
2. Re-establish each authorization capability from the current trusted conversation context. `authorization.md` is audit evidence only; if authority is unavailable, pause only before the first operation that requires that capability.
3. Classify every referenced worktree/branch as `MATCH`, `FAST_FORWARD_OWNED`, `DIRTY_RECOVERABLE`, `DIVERGED`, `BRANCH_REASSIGNED` or `MISSING` using the rules in `run-artifacts.md`. Never reset or silently adopt unexpected work.
4. Verify commits, packet hashes, resource leases, PR head SHA, CI results and deployed staging revision still exist and match.
5. Mark evidence stale when its code SHA, parent, packet, PR head or environment revision no longer matches, including dependent slice packets and downstream feature/CI/staging proof.
6. Reconstruct the next runnable DAG nodes.
7. Continue the repair or delivery loop without repeating already valid work.

## Completion report

Report:

- plan path and hash;
- final feature, integration and staging SHAs or artifact digests;
- selected profile, orchestration mode, terminal outcome, requested/resolved models and any automatic profile escalation;
- slices and their integrated commits;
- static, Sonar, review, E2E and CI evidence;
- staging deployment and E2E proof;
- context files created or refreshed;
- integration PR, production-target PR and authorized production deployment evidence when applicable;
- explicit exclusions, unresolved external blockers and production actions not taken.

After reporting, recommend [`task-closeout`](../task-closeout/SKILL.md) so the run's durable decisions, open obligations and negative lessons are emitted and federated instead of ending in the transcript. Closeout reads this run's evidence; it never rewrites it.

Do not claim completion before the selected terminal outcome is proved. A lower target such as `local-green` or `ready-pr` is valid only when it was explicitly selected; a higher target such as `production-green` never expands authority beyond the recorded envelope.
