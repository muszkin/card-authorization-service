# Artifact Contract and Agent Discovery

The generated map is a progressive-disclosure router. Keep the entrypoint short; store detail in focused artifacts and scoped directory contexts.

## Default layout

```text
context/
├── README.md                         # optional managed pointer; preserve other conventions
└── map/
    ├── INDEX.md                      # canonical human router
    ├── manifest.json                 # machine-readable scope, freshness, coverage, verdicts
    ├── project-overview.md
    ├── technology.md
    ├── architecture-and-flows.md
    ├── dependencies.md
    ├── documentation-index.md
    ├── delivery-and-verification.md
    ├── git-and-pr-history.md
    ├── risks-and-unknowns.md
    └── diagrams/
        ├── module-dependencies.mmd
        └── primary-runtime-flow.mmd

<meaningful-directory>/
└── .agents/
    └── project-context.md
```

Create optional artifacts only when evidence exists, for example `security-boundaries.md`, `data-model.md`, `deployment-topology.mmd`, or greenfield files. Add every artifact to the index and manifest. These are default paths, not authority to overwrite an existing foreign context convention; use the ownership and collision policy below.

## `context/map/INDEX.md`

Keep it concise and include:

1. project name, scope, operation, project classification, source revision/snapshot, generated/refreshed time, dirty-state caveat, and overall coverage/freshness verdict;
2. a statement that current user instructions and canonical code/runtime/docs outrank this generated map;
3. a task router:

| Consumer | Read next |
| --- | --- |
| research | overview, documentation index, useful related artifacts, relevant scoped contexts, unknowns |
| implementation planning | overview, technology, architecture/flows, dependencies, delivery, Git hotspots, risks/unknowns, useful related artifacts, relevant scoped contexts |
| review | architecture, contracts, risks, Git/co-change, delivery gates, prior review/health artifacts, relevant scoped contexts |
| `implementation-orchestrator` | approved plan and plan hash, overview, architecture/ADRs/flows, dependencies/contracts, delivery and quality-gate commands, risks, nearest scoped contexts, useful prior implementation/review/release artifacts |
| implementer | approved plan, relevant scoped contexts, dependencies, delivery commands, preserved invariants, applicable prior handoffs |

4. a top-level domain/workspace table with a link to the nearest `.agents/project-context.md`;
5. links to every central artifact and canonical project document;
6. the actual manifest path and instructions to use it for rolled-up/excluded directories;
7. unresolved contradictions and the freshness rule.

Do not repeat rankings or full tables from focused artifacts.

## `manifest.json`

Use valid JSON with this minimum shape:

```json
{
  "schema_version": 1,
  "generator": "project-context-initializer",
  "generator_version": "skill revision or content hash",
  "manifest_digest": {
    "algorithm": "sha256-rfc8785-with-empty-digest",
    "value": "sha256"
  },
  "operation": "initialize",
  "project_classification": "brownfield",
  "scope_kind": "repository",
  "phase": "complete",
  "generated_at": "RFC3339 timestamp",
  "repository_root": ".",
  "scope": ["."],
  "source_snapshot": {
    "basis": "HEAD+worktree",
    "revision": "commit or null",
    "branch": "branch or null",
    "worktree_status": [],
    "untracked_nonignored_paths": [],
    "comparability": "comparable",
    "non_comparable_reasons": [],
    "dirty_tracked_fingerprints": [
      {
        "path": "safe/dirty-file",
        "method": "sha256-current-worktree-bytes",
        "value": "sha256"
      }
    ],
    "inventory_fingerprint": "sha256 over normalized safe inventory",
    "fingerprint_method": "document the normalized inputs"
  },
  "git_history": {
    "available": true,
    "shallow": false,
    "commit_window": "record exact boundaries",
    "pr_provider": "provider or null",
    "pr_window": "record exact query or null"
  },
  "exclusions": [],
  "unreadable_paths": [],
  "directories": [
    {
      "path": "src/example",
      "coverage": "own",
      "context": "src/example/.agents/project-context.md",
      "reason": "domain and runtime boundary"
    },
    {
      "path": "src/example/types",
      "coverage": "rolled-up",
      "context": "src/example/.agents/project-context.md",
      "reason": "small structural leaf"
    }
  ],
  "remote_sources": [],
  "related_artifacts": [
    {
      "path": "context/foundation/example.md",
      "generator_or_owner": "other skill or Unknown",
      "purpose": "preserved prior knowledge",
      "scope": ["."],
      "artifact_type": "canonical-evidence|generated-synthesis|proposal|historical-record",
      "authority": "canonical|supporting|advisory|unknown",
      "source_revision": "commit or null",
      "generated_at": "RFC3339 timestamp or null",
      "freshness": "current|stale|unverifiable",
      "content_sha256": "sha256 or null",
      "approval_status": "not-applicable|draft|approved|rejected|superseded|unknown",
      "conflicts": [],
      "referenced_by": ["context/map/INDEX.md"],
      "consumers": ["implementation-planning", "implementation-orchestrator"]
    }
  ],
  "artifacts": [
    {
      "path": "context/map/project-overview.md",
      "ownership": "managed-block",
      "inputs": ["README.md"],
      "input_fingerprint": "sha256",
      "output_sha256": "sha256",
      "generated_at": "RFC3339 timestamp"
    }
  ],
  "verdicts": {
    "inventory": "complete",
    "coverage": "complete",
    "technology_and_documentation": "complete",
    "architecture_and_dependencies": "complete",
    "git": "partial",
    "agent_discovery": "complete",
    "scaffold": "not-applicable",
    "tests": "partial",
    "ci": "partial",
    "deploy_smoke": "not-applicable",
    "e2e": "partial",
    "freshness": "complete",
    "risks": "partial"
  },
  "verdict_details": {
    "git": {
      "reason": "remote PR evidence unavailable",
      "missing_evidence": ["remote PR history"],
      "impact": "local history only",
      "next_action": "query an authenticated read-only provider",
      "fallback": "retain local Git analysis",
      "owner": "Unknown"
    },
    "scaffold": {"reason": "brownfield project"},
    "tests": {
      "reason": "commands discovered but not run",
      "missing_evidence": ["current test result"],
      "impact": "runtime readiness is unverified",
      "next_action": "run documented focused tests",
      "fallback": "retain commands as unverified evidence",
      "owner": "Unknown"
    },
    "ci": {
      "reason": "configuration inspected without remote run access",
      "missing_evidence": ["current CI result"],
      "impact": "pipeline outcome is unverified",
      "next_action": "inspect the current provider run",
      "fallback": "report configuration-only evidence",
      "owner": "Unknown"
    },
    "deploy_smoke": {"reason": "no deployment requested"},
    "e2e": {
      "reason": "no authorized executable environment",
      "missing_evidence": ["real-surface E2E result"],
      "impact": "primary journey is unverified",
      "next_action": "run the documented E2E flow when authorized",
      "fallback": "retain static flow trace",
      "owner": "Unknown"
    },
    "risks": {
      "reason": "named unknowns remain",
      "missing_evidence": ["risk-owner decisions"],
      "impact": "some conclusions remain provisional",
      "next_action": "resolve the linked questions",
      "fallback": "keep risks visible in the index",
      "owner": "Unknown"
    }
  },
  "contradictions": [],
  "open_questions": [],
  "uncovered_paths": [],
  "next_actions": []
}
```

Add fields when useful, but do not remove the evidence/freshness fields. Compute `manifest_digest.value` by first setting that value to the empty string, canonicalizing the whole JSON document with RFC 8785 (JCS), hashing the resulting UTF-8 bytes with SHA-256, and then storing the lowercase hexadecimal digest. Recompute and compare it before treating a previous manifest as initializer-owned and unchanged. The digest is an integrity check, not authorization and not evidence that repository claims are trustworthy.

The inventory fingerprint must not read or hash secret contents; represent secret-bearing paths only by safe path/status metadata. A Git blob object ID is acceptable only for a clean tracked file whose current worktree bytes match that blob. For every safe dirty tracked file, hash the current worktree bytes; HEAD or index object IDs and a status letter are insufficient. For safe untracked inputs, use a current content hash. If a relevant dirty, untracked, secret-bearing, unreadable, or oversized input cannot be safely content-fingerprinted, set snapshot comparability to `non-comparable`, record the path/reason without its contents, and force an affected-scope rescan on refresh. Record remote provider/query/time/result identifiers or a safe result fingerprint when remote PR evidence affects an artifact.

`freshness=complete` requires a comparable current source snapshot and unchanged per-artifact inputs. A matching commit alone is insufficient. If the prior run had dirty/untracked inputs without a comparable snapshot, perform a full affected-scope rescan; if affected scope cannot be proven, perform a full scan. For a partial or blocked phase, keep the manifest valid and list uncovered paths and next actions.

## Ownership, collision, and path safety

Before each read or write, classify the target as:

- `absent`;
- `owned-unchanged`: previous manifest declares ownership and the current generated block/output hash matches;
- `owned-modified`: previous ownership exists but generated content changed outside this refresh;
- `unmanaged`: no initializer ownership marker or compatible manifest entry;
- `symlink`: classify further as in-repository, external, broken, or cyclic;
- `conflict`: path type or content cannot safely host the artifact.

Replace an entire file only when it is initializer-owned and unchanged. Markdown artifacts should normally use a managed block:

```markdown
<!-- BEGIN project-context-initializer:artifact -->
...generated artifact content...
<!-- END project-context-initializer:artifact -->
```

Preserve content outside the block. If an unmanaged central artifact occupies a default path, read and index it as project evidence, then either append a clearly bounded block when semantically safe or write the initializer artifact under `context/map/project-context/` and route the resolved index/manifest to it. If the default `INDEX.md` or `manifest.json` cannot be safely adopted, never overwrite it: use a namespaced entrypoint or stop for an ownership decision. A compatible initializer manifest may be replaced only after its generator/schema match and the exact `manifest_digest` algorithm above validates; an invalid or missing digest makes it `owned-modified` or `unmanaged`, never safely replaceable by assumption.

Treat paths from an existing manifest as untrusted hints. Re-derive write targets from the current inventory. Reject absolute paths, `..` traversal, paths escaping the resolved repository root, and symlinked path segments unless the user explicitly approves the exact target. Use `lstat` before following a link; record the link path and resolved target. Do not traverse external directory symlinks by default, and never write through an external, broken, or cyclic symlink.

## Central artifacts

### `project-overview.md`

Purpose, actors, product surfaces, repository/workspace boundaries, primary flows, current maturity, explicit non-goals, and a compact evidence map.

### `technology.md`

For each technology: role, declared version/range, locked or toolchain version, detected usage, provenance, decision state (`N/A`, `Proposed`, `Undecided`, `Decided`, `Accepted risk`, `Out of scope`), compatibility notes, and whether it was run.

### `architecture-and-flows.md`

Runtime entrypoints, vertical flows, module boundaries, data/state, integrations, async behavior, security/trust boundaries, observability, deployment shape, contradictions, and links to diagrams.

### `dependencies.md`

Human-readable dependency tree plus tables of module/package edges, inbound/outbound contracts, data/integration edges, cycles, high-risk hubs, and unknown edges. Link each material edge to evidence.

### `documentation-index.md`

Every discovered documentation/instruction path, scope, topic, authority, last-known freshness signal, read status, conflicts, and the artifact that uses it. Include a dedicated related-artifacts table for useful output from other skills/tools with generator/owner, status, and downstream routing. Recognize `context/implementation-runs/**` as execution evidence and route current run summaries, gate/review evidence, CI/staging proof and production-PR handoffs without adopting their ownership.

### `delivery-and-verification.md`

Commands and environments for setup/run/build/lint/typecheck/test/E2E/release/deploy/rollback/smoke. Mark each command `documented`, `derived`, `verified`, `failed`, or `not-run`. Describe real user surfaces and missing harnesses.

### `git-and-pr-history.md`

Exact history/PR windows, filters, hotspots, churn, co-change, hub caveats, recent directions, contributor/contact signals, PR evidence, CI/review signals, and limitations. Historical practice is not automatically a desired standard or ownership assignment.

### `risks-and-unknowns.md`

Contradictions, stale evidence, unreadable sources, unmodeled boundaries, security/operational risks, missing tests or deploy proof, impact, recommended next action, and required decision owner.

## Scoped `.agents/project-context.md`

Wrap generated content in these exact markers:

```markdown
<!-- BEGIN project-context-initializer:context -->
...generated section...
<!-- END project-context-initializer:context -->
```

Preserve everything outside the markers. The generated section contains:

- path, scope, source revision, refreshed time, coverage role;
- purpose and responsibilities;
- important files, entrypoints, public interfaces, and consumers;
- inbound/outbound dependencies and contracts;
- data, state, integrations, configuration names, and trust boundaries;
- run/build/test/E2E commands relevant to this scope;
- Git hotspots, co-change, and contact signals with caveats;
- invariants and behaviors not to break;
- risks, contradictions, and unknowns;
- child contexts and rolled-up child directories;
- exact evidence paths.

If an unmarked file already exists, append a managed section only when that preserves its meaning. Otherwise create a sibling `project-context.generated.md`, route the manifest to it, and report the conflict.

## Managed agent adapters

Use these exact sentinels:

```markdown
<!-- BEGIN project-context-initializer:router -->
...host-specific router...
<!-- END project-context-initializer:router -->
```

### Codex router

Add to the active root Codex instruction file:

```markdown
## Generated project context

Before repository-wide research, implementation planning, implementation orchestration, review, or implementation, read `<resolved-context-index>`. For work in a specific directory, follow its mapping to the nearest `.agents/project-context.md`. Treat generated context as navigation evidence and verify stale or high-risk claims against current code, runtime, and canonical documentation.
```

Replace `<resolved-context-index>` with the actual repository-relative path; do not leave a placeholder. Codex discovers at most one project instruction file per directory. If a non-empty `AGENTS.override.md` is active, update its managed block rather than an ignored sibling `AGENTS.md`. Otherwise use the active `AGENTS.md`; create root `AGENTS.md` only when no active project instruction file exists.

Inspect the effective root-to-CWD instruction chain and configured `project_doc_max_bytes` before writing. Before mutation, record the ordered effective instruction sources, loaded byte contribution, and a safe checksum or tail anchor for the content currently visible in each representative mapped CWD. Simulate the exact post-edit chain. Put the router near the beginning of the active file, after unavoidable frontmatter/title, only when every previously loaded source and byte range remains visible. If the adapter would truncate or drop any previously loaded instruction, do not write it (or restore the exact pre-edit file), mark Codex discoverability `partial` or `blocked`, and propose an explicit byte-limit increase or instruction refactor for user approval.

Codex rebuilds the chain only in a fresh run/session. Discoverability cannot be `complete` until a fresh read-only Codex session in the target CWD confirms both the active instruction sources/resolved context entrypoint and the preserved pre-edit tail anchors. A smoke that sees only the new router is insufficient; unavailable or denied smoke is `partial`.

### Claude Code router

In root project-shared `CLAUDE.md`, use:

```markdown
## Generated project context

@<relative-resolved-context-index>
```

Replace the placeholder with the actual path. In `.claude/CLAUDE.md`, the relative import is normally `@../context/map/INDEX.md`; compute it from the containing file. If both shared project locations exist, respect the established project convention and avoid duplicate imports. Never place a shared router in `CLAUDE.local.md`. If no shared Claude project instruction exists, create a minimal root `CLAUDE.md` with `@AGENTS.md` when the in-repository AGENTS file is safe to import, followed by the managed context import.

Claude Code expands unquoted `@path` imports. Do not wrap the import in backticks. Avoid importing every detailed artifact; the concise index routes further reads. Inspect project settings, setting sources, and `claudeMdExcludes`. Discoverability cannot be `complete` until a fresh Claude Code session in the target CWD reports the selected shared instruction file and imported resolved index in `/context` or equivalent read-only diagnostics; unavailable or denied smoke is `partial`.

If `CLAUDE.md` symlinks to `AGENTS.md`, do not add Claude-only import syntax to a separate replacement file. The shared Codex router remains a valid instruction to read the index.

### Nested adapters

When a meaningful directory already has a host instruction file, add a managed pointer to its local `.agents/project-context.md`. Do not create thousands of nested instruction files. Root routers plus the manifest remain the universal discovery path.

## `context/README.md`

Use these markers:

```markdown
<!-- BEGIN project-context-initializer:context-readme -->
...small pointer to the resolved context index...
<!-- END project-context-initializer:context-readme -->
```

If this file exists, append or refresh only that managed section. If it does not exist and the repository has no competing context convention, create it. Never overwrite other 10x, Maister, project, or maintainer content and never stage the entire `context/` directory automatically.
