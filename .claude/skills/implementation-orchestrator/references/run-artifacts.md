# Durable run artifacts

Keep coordination state discoverable, resumable and conflict-free.

## Default location

Use:

```text
context/implementation-runs/<run-id>/
```

If repository policy requires another location, record the resolved location in `context-reconciliation.md` and let a `project-context-initializer` refresh federate it into the resolved context index (default `context/map/INDEX.md`). Do not directly edit initializer-owned routing. If `context/` is ignored by Git, keep it in the repository root checkout and pass absolute packet/evidence paths to worktree agents.

The `run-id` should be stable, sortable and collision-resistant, for example a UTC timestamp plus feature slug.

## Layout

```text
context/implementation-runs/<run-id>/
├── RUN.md
├── run.json
├── events.jsonl
├── orchestrator-lease.json
├── authorization.md
├── baseline.md
├── topology.md
├── scheduler.md
├── resources.md
├── slices/
│   └── <slice-id>/
│       ├── packet.md
│       ├── state.json
│       ├── worker-report.md
│       ├── gates.md
│       ├── review.md
│       ├── integration.md
│       └── evidence/
├── integration/
│   ├── history.md
│   ├── combined-gates.md
│   └── combined-review.md
├── delivery/
│   ├── integration-pr.md
│   ├── ci.md
│   ├── staging.md
│   ├── production-pr.md
│   └── production.md
└── context-reconciliation.md
```

Avoid duplicating large logs. Store a stable path, URL, checksum and concise interpretation.

## Ownership

- Only the orchestrator writes `run.json`, `events.jsonl`, scheduler, resources, integration and delivery records.
- Workers write only their unique slice-local report/evidence files.
- Reviewers return a report that the orchestrator stores under the relevant slice or combined-review path.
- Use atomic replacement for mutable JSON state.
- Append events; never rewrite history to hide a failed attempt.

This avoids parallel agents racing on a shared plan or progress file.

Before any central mutation, acquire one exclusive orchestrator lease using a host-supported atomic lock mechanism and record a random lease token, owner/session identity, acquisition/renewal time and run-state revision in `orchestrator-lease.json`. Renew it while the orchestrator is active and release it explicitly at terminal handoff. On resume, inspect actual owner activity plus the recorded revision; never take over solely because a timestamp looks old. A takeover requires proof the previous owner is inactive, an atomic new lease, and an append-only `ORCHESTRATOR_TAKEOVER` event. If ownership cannot be proven, keep the audit read-only rather than allowing two orchestrators to integrate concurrently.

## `run.json` minimum contract

The machine-readable contract is [schemas/run-schema.json](../schemas/run-schema.json). It is the source of truth for required fields and allowed enum values; the example below is the same contract in readable form and is checked against it. Extra properties are allowed so a host may record more, but a property named in the schema keeps its meaning.

Validate the ledger with the script shipped beside the schema rather than by eye, after every central state change and on every resume:

```bash
node <skill-path>/scripts/validate-run.mjs context/implementation-runs/<run-id>/run.json
```

It exits non-zero with one finding per line. A ledger that does not satisfy its own contract cannot be trusted to describe what a resume may skip.

Keep the structure portable across agent hosts:

```json
{
  "schema_version": 2,
  "run_id": "2026-08-28T120000Z-feature-slug",
  "state": "SLICES_RUNNING",
  "plan": {
    "path": "/absolute/path/to/plan.md",
    "sha256": "...",
    "approved_base_sha": "..."
  },
  "context": {
    "index_path": "/absolute/path/context/map/INDEX.md",
    "manifest_path": "/absolute/path/context/map/manifest.json",
    "manifest_digest": "...",
    "source_revision": "...",
    "inventory_fingerprint": "...",
    "generated_at": "2026-08-28T11:00:00Z"
  },
  "execution_contract": {
    "profile": "standard",
    "orchestration": "fire-and-forget",
    "terminal_outcome": "production-pr-ready",
    "selection_source": "operator-confirmed",
    "confirmed_at": "2026-08-28T11:58:00Z",
    "automatic_profile_escalation": true,
    "model_policy": {
      "policy": "daily-coding",
      "worker_requested": "gpt-5.6-terra",
      "worker_resolved": "gpt-5.6-terra",
      "worker_reasoning": "medium",
      "reviewer_requested": "gpt-5.6-terra",
      "reviewer_resolved": "gpt-5.6-terra",
      "reviewer_reasoning": "medium"
    },
    "profile_history": []
  },
  "authorization_path": "authorization.md",
  "topology_path": "topology.md",
  "feature": {
    "branch": "feat/example",
    "worktree": "/absolute/path",
    "head_sha": "..."
  },
  "slices": {},
  "deferred_obligations": [
    {
      "gate": "e2e",
      "scope": "slice-01",
      "status": "DEFERRED_TO_FINAL",
      "required_at": "FINAL_FEATURE_GATES",
      "must_pass_on": "assembled_feature_sha"
    },
    {
      "gate": "sonar",
      "status": "DEFERRED_TO_CI",
      "required_at": "INTEGRATION_PR",
      "required_check": "Sonar Quality Gate",
      "must_pass_on": "current_pr_head_sha"
    }
  ],
  "integration_pr": null,
  "staging": {
    "status": "NOT_RUN",
    "expected_source_sha": null,
    "expected_artifact_digest": null,
    "observed_source_sha": null,
    "observed_artifact_digest": null
  },
  "production_pr": null,
  "production": {
    "status": "NOT_RUN",
    "expected_artifact_digest": null,
    "observed_artifact_digest": null,
    "verification_path": null,
    "observation_window": null,
    "rollback_status": "NOT_NEEDED"
  },
  "updated_at": "2026-08-28T12:00:00Z"
}
```

Do not store secrets or credentials.

## Slice state

Each `state.json` records:

- slice ID and logical owner;
- state and state revision;
- branch, worktree, parent/head SHA;
- packet path/hash;
- dependency IDs;
- owned/forbidden paths;
- resource lease IDs;
- gate statuses and evidence paths;
- active failure classification and repair attempt lineage;
- integration commit and feature-head verification;
- timestamps.

Gate statuses use the contract in `quality-gates.md`. A code SHA change automatically makes prior downstream results `STALE`.

Each slice also records the execution profile and cadence inherited by its packet, requested/resolved worker model and reasoning effort, permitted `DEFERRED_TO_FINAL` obligations, and any escalation trigger. Never mutate history to make a quick slice appear to have passed a gate it deferred.

## Append-only events

Write one JSON object per line with:

```json
{
  "at": "2026-08-28T12:00:00Z",
  "actor": "orchestrator",
  "slice": "slice-02",
  "event": "GATE_FAILED",
  "from": "SONAR_ACCOUNTED",
  "to": "IMPLEMENTED",
  "sha": "...",
  "evidence": "slices/slice-02/evidence/sonar-003.txt",
  "reason": "new-code coverage below repository Quality Gate"
}
```

Events must explain repair routing, stale evidence, reassignments, integrations, CI head changes and staging revisions.

Record execution decisions explicitly. Use events such as `EXECUTION_CONTRACT_SELECTED`, `MODEL_RESOLVED`, `PROFILE_ESCALATED`, `TERMINAL_OUTCOME_REACHED` and `PRODUCTION_ROLLBACK_TRIGGERED`. A profile escalation event contains the old/new profile, evidence, whether it was automatic under the recorded rule, affected packets and newly required gates. No automatic profile downgrade is valid.

## `RUN.md`

Maintain a compact human-readable router:

- objective and current state;
- selected profile, orchestration mode, terminal outcome and automatic-escalation rule;
- requested/resolved worker and reviewer models;
- plan and context links;
- feature branch/head;
- slice table with owner/state/head;
- active failures/blockers;
- integration PR/CI/staging/production PR;
- production deployment/verification when `production-green` is selected;
- next executable action;
- pointers to detailed evidence.

Do not treat `RUN.md` checkboxes as authoritative; `run.json`, Git and external state must agree.

## Resume audit

Before continuing:

1. validate JSON/JSONL syntax;
2. acquire or safely take over the exclusive orchestrator lease;
3. re-establish each authorization capability as `confirmed-current`, `needs-reconfirmation` or `excluded` from trusted current conversation context; never promote the audit file itself to authority;
4. verify plan hash and approved base;
5. classify every worktree/branch:
   - `MATCH`: path, branch, ancestry, SHA and expected status agree; reuse it;
   - `FAST_FORWARD_OWNED`: ancestry, diff and owned paths prove only the assigned worker advanced it; adopt the new candidate SHA and stale all SHA-bound gates;
   - `DIRTY_RECOVERABLE`: preserve changes without reset and return it to the same logical owner to finish/recover;
   - `DIVERGED`: do not adopt or reset; preserve evidence and create a fresh repair worktree from the last trusted SHA;
   - `BRANCH_REASSIGNED`: do not touch it; preserve evidence and create a correctly assigned worktree;
   - `MISSING`: recreate only from a verified branch/commit, otherwise route to recovery;
6. reconcile every resource lease against reality as described in [resource-isolation.md](resource-isolation.md) before reuse;
7. compare gate evidence SHA to current candidate SHA;
8. compare CI results to current PR head;
9. compare deployed staging revision to expected artifact;
10. mark mismatches `STALE` and append an event;
11. when feature HEAD changes, also stale descendant parent/packet assumptions, combined gates, prior PR/CI results for another head, and staging proof/E2E for another deployed revision;
12. rebuild the runnable slice set from the DAG.

If a capability is `needs-reconfirmation`, continue all independently authorized/read-only work and pause only immediately before the first side effect requiring that capability.

## Context federation

Before freezing the final feature candidate and again at delivery completion, invoke or follow `project-context-initializer` refresh rules so downstream research, planning, review and implementation can discover:

- the implementation run router;
- architecture/ADR changes;
- updated directory context;
- integration and staging evidence;
- the production PR handoff.
- authorized production artifact, verification, observation and rollback evidence when `production-green` was selected.

Link useful pre-existing artifacts instead of overwriting or duplicating them.

If delivery-time federation would change tracked integration-branch files after their verified SHA, do not mutate that branch silently. Keep the reconciliation record SHA-bound and route the tracked documentation change through the repository's normal gated change path or an explicitly appropriate production handoff.
