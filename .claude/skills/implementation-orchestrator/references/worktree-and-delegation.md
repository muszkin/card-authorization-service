# Worktree and delegation protocol

One orchestrator owns coordination. Every implementation or repair assignment runs in an isolated Git worktree. Both orchestration modes are multi-agent; they differ in when the operator is asked to intervene.

## Role boundaries

### Orchestrator

Owns:

- plan and context intake;
- authorization and topology records;
- slice DAG, scheduler and resource leases;
- central run state and append-only events;
- creation and validation of worktrees;
- integration lock and feature-branch history;
- PR, CI, staging and authorized production coordination;
- execution-profile, model-policy and terminal-outcome enforcement;
- routing failures back to logical owners.

The orchestrator does not edit product code or self-review an implementation.

### Slice worker

Owns one slice or repair packet in one worktree. It may:

- read its packet, named context and repository instructions;
- implement and test its owned change surface;
- update slice-local evidence and handoff files;
- create focused commits containing only owned changes.

It may not:

- write the central run state;
- modify another worker's worktree;
- merge, push, open/merge PRs or deploy;
- change the approved plan or architecture;
- spawn or impersonate the independent reviewer;
- silently absorb unrelated cleanup.

### Independent reviewer

Receives an exact immutable diff, plan acceptance criteria and relevant context. Give it a separate clean read-only review worktree or detached checkout pinned to the candidate SHA; never point it at the worker's mutable worktree. It reports findings to the orchestrator and does not edit code. Do not prime it with the worker's claim that the code is correct.

## Integration worktree

Create a dedicated worktree for the feature-integration branch from the pinned approved base SHA. Do not reuse a dirty user checkout.

Before provisioning, inspect `git worktree list --porcelain`, repository worktree conventions and ignore rules. Use an established ignored worktree parent or a safe sibling/temporary parent outside the tracked tree; never create an unignored nested worktree that pollutes project inventory. Initialize submodules and worktree-local tooling only through repository-documented, authorized commands. If safe worktree provisioning is unavailable, use `BLOCKED_EXTERNAL`; never fall back to concurrent agents editing one checkout.

Record:

- absolute path;
- feature branch;
- base branch and pinned base SHA;
- integration target;
- repository merge policy;
- integration lock state.

Only the orchestrator writes this worktree.

## Slice branches and worktrees

Use deterministic, collision-resistant names derived from the feature and slice ID, following repository/Jira conventions where present.

Every assignment record includes:

- branch and absolute worktree path;
- parent SHA;
- logical owner ID;
- owned and forbidden paths;
- resource lease;
- slice packet path and hash;
- current head SHA;
- cleanup eligibility.

Validate that the branch begins at the intended parent SHA and the worktree has no unexplained changes before dispatch.

At worker start, every resume, immediately before creating a candidate commit, and immediately before every gate command, require an identity guard that records and compares:

- `git rev-parse --show-toplevel` with the assigned absolute worktree path;
- current branch with the assigned branch;
- `git rev-parse HEAD` with the expected parent or candidate SHA;
- `git status --porcelain` with the expected clean or explicitly recorded changes;
- packet hash and resource lease IDs.

Abort that assignment on mismatch and let the orchestrator reconcile it. Do not continue in the caller's current checkout.

## Self-contained slice packet

The orchestrator writes `slices/<slice-id>/packet.md` with:

1. objective and user-visible value;
2. exact plan excerpt and plan hash;
3. acceptance scenarios and expected initial RED;
4. architecture/ADR constraints;
5. relevant root and nearest directory instructions;
6. context paths and linked documentation to read;
7. owned change surfaces and public contracts;
8. dependency and consumer obligations;
9. commands with working directories;
10. static, Sonar, review and E2E gate requirements;
11. selected profile and per-slice cadence, including every `DEFERRED_TO_FINAL` obligation allowed by a quick profile;
12. requested and resolved worker model plus reasoning effort, with allowed fallback/escalation;
13. orchestration mode, terminal outcome and automatic profile-escalation rule;
14. runtime resource lease;
15. integration test required after merge;
16. explicit prohibitions and authorization boundaries;
17. required worker report format.

Use absolute context and packet paths. Generated `context/` may be ignored by Git and therefore absent from a worktree; do not assume the worker can discover it through its checkout.

## Resource lease

The resources, the lease state machine and the release discipline are defined once in [resource-isolation.md](resource-isolation.md). Read it before allocating anything, and record the effective bound resources in the assignment record.

The orchestrator owns every lease: it probes and reserves before dispatch, and no worker allocates a shared resource for itself.

## Parallel-cohort proof

Two slices may run concurrently only when all answers are yes:

- no dependency path exists between them;
- their owned files and generated outputs do not overlap;
- they do not change the same public contract or migration sequence;
- their runtime leases are isolated;
- neither requires the other's integrated behavior for its acceptance test;
- their integration order is either commutative or explicitly defined;
- the plan identifies how combined behavior will be tested.

Record this proof in the scheduler. Otherwise run sequentially.

## Scheduling

### Fire-and-forget

`fire-and-forget` is the default. It means the orchestrator autonomously schedules dependency-ready work, waits on useful completion conditions, routes failed gates back to owners, integrates verified candidates and continues delivery until the recorded terminal outcome or a legitimate external/authority blocker. It does not mean launching every slice at once, abandoning agents after dispatch, suppressing evidence, bypassing approvals or widening scope.

Keep concurrency bounded by available agents and proven resource independence. Prefer small self-contained packets, event/condition waits and durable state over repeated narration or polling. Do not pause for routine red gates, expected repair iterations, slice completion, clean integration or green CI when those actions remain inside the envelope.

### Supervised

`supervised` uses the same worktree isolation, subagent ownership and gates, but pauses at the operator-selected checkpoints. If none were specified, use: before first product edit, before first remote push, before integration merge, before staging mutation and before any production action. A pause does not make the orchestrator the implementer.

### Model assignment

Resolve every model through the procedure in [execution-profiles.md](execution-profiles.md) before dispatch, and record the requested and resolved identifiers in the assignment. Use low reasoning for quick profiles and medium for `standard`/`bug` by default; use an independent reviewer at medium reasoning. Preserve explicit operator overrides, record fallbacks, and escalate to a stronger model only after evidence shows the default is insufficient or the operator requested it.

Use the plan DAG, not file count, to determine readiness:

1. choose nodes whose dependencies are integrated and feature HEAD is green;
2. form only proven-independent cohorts;
3. reserve resources before dispatch;
4. integrate completed slices one at a time under the integration lock;
5. schedule feature-head verification and final feature gates as resource-owning DAG nodes with their own leases; serialize them against any worker holding a conflicting lease from the isolation contract;
6. refresh descendants' parent SHA and contracts after every integration;
7. do not let a stale worker merge itself.

In `fire-and-forget`, continue this scheduler without routine operator prompts until the terminal outcome is proved. In `supervised`, stop only at the recorded checkpoints. In either mode, an unexpected high-risk surface or scope expansion triggers the challenge/escalation rule rather than silent continuation.

If a slice discovers a contract change affecting another active worker, pause only those conflicting nodes, update their packets and require them to rebase/revalidate.

## Repair ownership

Route a slice-local failure to the same logical owner so diagnostic context is retained. Route a cross-slice or integration-contract failure to the owner of the narrowest responsible boundary, with affected owners available for evidence.

After a slice has been squash-integrated, do not repair from its obsolete branch. Create a fresh repair worktree from current feature HEAD and retain the same logical owner in the ledger.

## Handoff report

The worker returns:

- branch, worktree and final commit SHA;
- files intentionally changed;
- acceptance RED evidence;
- gate commands, statuses and evidence paths;
- architecture/contract decisions made within the packet;
- independent review reference and resolved findings;
- remaining risks or external blockers;
- suggested integration tests.
- selected profile, requested/resolved model, any fallback and any discovered profile-escalation trigger.

The orchestrator verifies the commit and evidence directly. A worker's summary is not proof.

## Cleanup

Do not remove a worktree until:

- its commits are integrated or deliberately superseded;
- evidence is copied into the durable run directory;
- no pending repair references its branch;
- final feature and delivery checks no longer need it.

Use repository-safe, recoverable cleanup and never delete unrelated branches or user worktrees.
