# Git, PR, Dependency, and Diagram Analysis

Record raw measures and caveats. Avoid opaque scores, arbitrary ownership claims, and graphs that imply certainty beyond the evidence.

## Git preflight

Record:

- current branch and revision;
- whether history is shallow or incomplete;
- repository age and exact analyzed range;
- commit count and any path scope;
- mailmap/alias normalization used;
- exclusions for bots, generated/vendor/lock files, binaries, mass-format commits, merges, or unusually large commits.

If history is insufficient, state the limitation and rely more heavily on current structure, docs, CI, and user confirmation.

## Territory and hotspot analysis

Produce raw, explainable views:

- commits touching each meaningful module and file;
- lines added/deleted when meaningful and not dominated by generated files;
- distinct active periods or quarters, noting partial boundary periods;
- recency of changes;
- recurring co-change pairs or clusters calculated per commit;
- hub files that connect many changes;
- current tracked-file validation for historical paths.

Group by real workspace/domain boundaries rather than an overly broad top-level directory. Separate recurring coupling from one giant commit. Report generated/mock/vendor/lock co-change separately from manual domain edits.

History reveals activity and likely support contacts, not formal ownership or desired architecture. Combine it with CODEOWNERS, docs, blame, current contributors, and runtime/testability risk before suggesting whom to ask.

## PR and review history

Use only an already authenticated, read-only provider or locally available merge metadata. Do not install a CLI, trigger authentication, or expose remote data without permission.

Record:

- provider/repository and query mechanism;
- time or count window and why it is representative;
- merged/open/closed filters;
- PR numbers/URLs or local merge identifiers;
- files, modules, topics, migrations, incidents, and recurring review feedback examined;
- bots and automated changes excluded;
- unavailable fields and API truncation.

Analyze which areas change together, the direction of recent product work, migration patterns, release/rollback lessons, and CI/review rules. A pattern seen in code or reviews is `Observed practice`; it becomes a standard only when documented, enforced, or user-approved.

## Dependency model

Build multiple evidence lanes:

1. workspace/package dependencies from manifests and lock/workspace configuration;
2. source-module dependencies from imports, includes, DI/container wiring, build targets, or native architecture tools already present;
3. runtime flow from routes/commands/events/jobs through application/domain logic to state and outputs;
4. data and integration dependencies: databases, migrations, queues, caches, files, services, third parties;
5. delivery dependencies: build artifacts, images, infrastructure, secrets names, environments, deploy order, smoke and rollback.

Prefer a project-native analyzer already present and compatible with the dominant language. Do not install a generic graph tool merely because it is familiar. State whether each edge is statically observed, runtime observed, inferred, or unknown.

For very large graphs, publish a high-level graph and focused subgraphs that answer concrete questions. Preserve cycles and unknowns; do not erase them to make the diagram tidy.

## Human-readable dependency tree

In `dependencies.md`, include a compact tree or hierarchy plus an edge table:

| From | To | Contract or reason | Direction | Evidence | Status |
| --- | --- | --- | --- | --- | --- |

Also identify:

- public contracts and compatibility boundaries;
- high fan-in/fan-out hubs;
- cycles and shared mutable state;
- generated edges and runtime-only edges;
- boundaries without enough evidence.

## Mermaid source

For a project with meaningful module dependencies and runtime behavior, create at minimum:

- `module-dependencies.mmd` using `flowchart LR`;
- `primary-runtime-flow.mmd` using `flowchart LR` or `sequenceDiagram`.

For `greenfield-empty`, documentation-only work, or a genuinely single-component project where a graph would invent or obscure relationships, mark the relevant diagram verdict `not-applicable` with a reason. Do not draw proposed edges as current architecture.

Use stable ASCII node IDs and quoted human labels. Group workspaces or layers with `subgraph`. Use a legend or edge labels for observed, inferred, external, and unknown relationships. Do not place secrets or production identifiers in diagrams.

Keep each diagram reviewable. Split it when it approaches roughly 25-35 nodes or when unrelated flows obscure the primary question. Link focused diagrams from the architecture artifact.

Validate syntax with an existing Mermaid renderer or project documentation build when available. Do not install one only for validation without approval. Otherwise use conservative syntax, validate links and node references manually, and mark rendering `not-run`.
