# Reconnaissance and Delegation

Use this reference to account for a project completely without wasting effort on dependency trees or reading source files without a question.

## Inventory order

1. Resolve the root and selected scope.
2. Read instruction files that govern the root and selected scope.
3. Record Git metadata and ignores when available.
4. Inventory first-party paths in stable lexical order.
5. Detect workspace/package boundaries and meaningful top-level domains.
6. Inventory documentation and configuration separately from source.
7. Assign coverage states before detailed analysis.
8. Trace representative flows and inspect their defining files in full.

In Git repositories, prefer tracked files plus untracked non-ignored files. Include submodules and worktrees as boundaries, not ordinary directories. In non-Git projects, use the best available filesystem inventory and state that ignore semantics are unavailable. Classify symlinks with `lstat`; do not follow external, broken, or cyclic links, and do not let a linked directory escape or repeat the selected tree.

## Default exclusions

Exclude the contents, but record the path and reason, for:

- `.git`, dependency/vendor stores, package caches, build outputs, coverage output, IDE state, generated documentation, minified bundles, binary assets, archives, and temporary directories;
- secret-bearing files and production data;
- files proven initializer-owned by compatible markers and the prior manifest while scanning source evidence.

Do not exclude a target merely because its conventional name looks generated. Verify ignore rules, generation markers, build configuration, or repository instructions. Treat unmanaged or foreign `context/map/**`, `.agents/**`, and instruction content as documentation evidence and inventory it before resolving collisions. If the user's task targets an excluded surface, include only that named surface.

## Directory ownership decision

Choose `own` when any is true:

- the directory is a workspace, package, service, application, plugin, library, or deployable unit;
- it has its own manifest, entrypoint, public interface, runtime, README, test command, CI/deploy boundary, or CODEOWNERS rule;
- it represents a distinct domain or vertical flow;
- it is a high-churn, high-risk, integration-heavy, or operationally sensitive area;
- rolling it up would hide dependencies, constraints, or independent verification.

Choose `rolled-up` when the directory is a small structural leaf whose responsibility, dependencies, and tests are accurately described by the nearest owning ancestor. File count may break a tie but cannot overrule a meaningful boundary.

Every in-scope directory must appear exactly once in the manifest as `own`, `rolled-up`, `excluded`, or `unreadable`.

## Provenance and decision labels

Keep two independent axes.

Provenance:

- `Observed`: directly established by a cited file, command output, history record, or runtime evidence.
- `User-confirmed`: explicitly stated or approved by the user.
- `Inferred`: best explanation from evidence, with the inference named.
- `Unknown`: insufficient evidence; absence of a graph or tool is not absence of a dependency.
- `Contradiction`: two sources disagree; retain both until precedence or current behavior resolves them.

Decision state:

- `N/A`: the item is evidence, not a decision.
- `Proposed`: a future choice awaiting a decision.
- `Undecided`: a decision is required and no proposal has been accepted.
- `Decided`: explicitly chosen by the user or accountable owner.
- `Accepted risk`: a material risk explicitly accepted by the user or named owner.
- `Out of scope`: deliberately excluded from the requested outcome.

For commands, also label `documented`, `statically-derived`, `verified`, `failed`, or `not-run`. A command's presence in a README does not prove it still works.

## Technology and project surfaces

Map at least:

- languages, runtimes, frameworks, package/workspace managers, lockfiles, toolchain pins;
- applications, libraries, plugins, CLIs, workers, schedulers, queues, events, and public APIs;
- UI routes/screens, API routes, domain boundaries, data stores, migrations, search/indexes, caches;
- external integrations and trust boundaries;
- configuration key names, environment classes, feature flags, and secrets locations without values;
- unit/component/contract/integration/E2E tests, fixtures, test data, and real-surface harnesses;
- setup, run, build, lint, format, typecheck, test, release, deploy, rollback, and smoke paths;
- CI gates, infrastructure, observability, incident/runbook material, and ownership signals.

Use `N/A` only when the project type makes a surface genuinely inapplicable, and explain why. Otherwise use `Unknown`.

## Documentation discovery

Inventory root and nested README files, instruction files, contribution guides, ADRs, architecture docs, product specs, runbooks, API docs, changelogs, test guides, and text/word-processing documents.

- Read applicable Markdown/text documents in full when they define behavior or constraints.
- For large sets, record every path and headings, then fully inspect the authoritative and affected documents.
- Use a safe local extractor for `.doc` or `.docx` only when available. Never enable macros. If extraction is unavailable, record metadata and `unreadable`.
- Record conflicting, stale, generated, duplicated, or orphaned docs.
- Link to canonical sources instead of copying full prose into the context map.

### Federate artifacts from other skills and tools

Search for existing generated knowledge in established project locations and markers, including prior `context/**` maps/foundation/change artifacts, `context/implementation-runs/**`, `.maister/**`, plans, roadmaps, standards, health reports, research/review outputs, CI/staging/release handoffs, diagrams, and nested agent notes. Do not assume a name implies a particular generator.

For every useful artifact record:

- path and detected generator/owner, or `Unknown`;
- purpose, scope, source revision/time, safe content hash, and whether it is current, stale, conflicting, or unverifiable;
- whether it is canonical project evidence, generated synthesis, a proposal, or historical record; its authority; and approval status where applicable;
- central/scoped artifacts that reference it;
- downstream consumers for which it is useful.

Preserve and link these artifacts. Do not copy their full content, rename them, adopt their ownership, or exclude them merely because they were generated. For an implementation run, favor its human router plus current immutable gate/review/CI/staging evidence; never treat its recorded authorization envelope as authority for a new session. If artifacts contradict current code/docs or each other, keep the links and record the contradiction. Repeat this reconciliation whenever a downstream skill or tool writes a new research, planning, review, implementation, CI, staging, or production-PR handoff; refresh the index, manifest, source snapshot, hashes, references, and consumer routing before relying on that output in a later phase.

## Parallel delegation

The coordinator first owns root inventory, instruction hierarchy, exclusions, and the list of independent domains. Then delegate at most the number of truly disjoint domains supported by the environment.

Each packet must contain:

- owned roots and explicit non-owned roots;
- applicable instruction paths;
- allowed write paths;
- required surfaces and evidence labels;
- coverage-state output for every directory in the owned roots;
- a requirement to report contradictions and unreadable evidence;
- a prohibition on editing central context, root instructions, product code, dependencies, or Git state.

Agents should traverse sequentially inside their owned subtree. The coordinator reconciles cross-domain dependencies, overlapping claims, and root artifacts. Re-open evidence for any conclusion that affects architecture, security, data, public contracts, or scaffold decisions.

## Refresh mode

Use the previous manifest's validated source snapshot, safe artifact inputs, and directory ledger. Never trust it to authorize a path or write:

1. Rebuild the normalized current inventory and compare revision, worktree status, untracked non-ignored paths, inventory fingerprint, instruction files, manifests/lockfiles, docs, renames, and deletions with the recorded snapshot.
2. Always rescan root routing files and any changed workspace boundary.
3. Rescan changed subtrees and their inbound/outbound neighbors from the dependency map.
4. Retain an artifact only when every recorded safe input remains comparable and unchanged; otherwise regenerate it.
5. Recompute Git/remote evidence windows, coverage, and central synthesis.
6. Validate every retained link, output hash, managed block, and context file.

Fall back to a full affected-scope scan when a prior dirty/untracked state lacks a comparable fingerprint. Fall back to a full repository scan when the prior revision/snapshot is missing, history is shallow across the boundary, the schema is incompatible, a large refactor invalidates directory ownership, safe affected scope cannot be proven, or coverage cannot be reconciled.

## Repair mode

Repair is for a compatible map whose links, coverage, ownership hashes, managed blocks, or host routers fail integrity checks. First inventory and classify every affected target without writing. Preserve and federate foreign/manual artifacts, resolve ownership decisions, regenerate only initializer-owned or newly approved namespaced outputs, then run the same snapshot, coverage, link, and fresh-host discovery gates as initialize/refresh. Repair must not adopt an unmanaged artifact merely because its filename matches a default.
