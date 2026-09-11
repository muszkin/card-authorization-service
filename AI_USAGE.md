# How AI was used to build this

This repository's initial implementation was produced with an AI coding agent (Claude Code) working from a
written plan, under the rules below. A subsequent Codex repair for numeric input boundaries is recorded in the
[safe-numeric-boundaries run](context/implementation-runs/20260911-safe-numeric-boundaries/); it first reproduced
the unit and HTTP acceptance regressions, then corrected the narrow input conversion. The point of this file is
not to say that AI was used, which is unremarkable, but to show where its output was constrained, what it
proposed that was turned down, and how the result was checked.

## What was decided before any code existed

The plan handed to the agent fixed the things that matter: the domain (a card authorization service with a
ledger-derived balance), the four-layer package structure with ArchUnit as the enforcer, the six endpoints, the
decision rules and their order, fail-closed on a silent risk engine, the test pyramid as separate Gradle suites,
Cucumber scenarios in business language, and a commit-by-commit sequence in which every `test:` commit leaves
the build red and the following `feat:` commit turns it green. The agent did not choose what to build; it chose
how to write it within those limits.

## What the agent did

- Wrote every test first, ran it, and confirmed it failed for the expected reason before writing production code.
  For new types the red build is a test-compilation failure; for new behaviour on existing types it is an
  assertion failure (the concurrency test approved 11 of 10 affordable purchases before the lock existed).
- Checked library versions and APIs against Maven Central and the actual jars rather than from memory. Spring
  Boot 4.1 with Jackson 3, Testcontainers 2 and JUnit 6 renamed enough things that this mattered.
- Proposed three design refinements that were accepted: a per-card advisory lock instead of `SELECT ... FOR
  UPDATE` on the card row, so the cached card stays the source of truth on the hot path; `JdbcClient` instead
  of JPA for an immutable, append-only model; and an explicit `CardLookup` instead of `@Cacheable`, so the
  cache-aside pattern is readable and the adapters stay independent.
- Ran the full build after every step, the acceptance suite over HTTP, and a manual `bootRun` against
  `compose.yaml` before writing the README instructions.

## What was rejected

- **Generating the service first and adding tests afterwards.** Tests written after code pass immediately and
  prove nothing about their own sensitivity. Every red build in the history was observed, not assumed.
- **A `SERIALIZABLE` transaction as the concurrency guard.** Correct, but it resolves conflicts by aborting,
  which turns a hot card into a retry storm. Rejected in favour of a lock plus an explicitly reasoned
  READ COMMITTED (see `DECISIONS.md`, entry 4).
- **JSpecify `@NullMarked` on every package.** Without a null checker in the build it is decoration that
  suggests a guarantee nobody enforces.
- **Spring's cache abstraction, Micrometer metrics and tracing, OpenAPI generation, a custom exception
  hierarchy per layer, typed identifiers.** Each was proposed as "cheap to add"; each was cut because the brief
  says small and none of them changes a design decision.
- **Retrying outbound calls after a timeout.** It would double the worst case and blow the 200-300 ms
  authorization budget; only an immediate 429/503 is retried, once.
- **Caching cache misses.** An unknown card id could shadow a card issued a moment later.

## Where the agent was wrong, and how it was caught

- Spring Initializr returned a server error for a Boot 4.1 Gradle project, so the skeleton was written by hand
  from the documented starter names, and each starter's existence was verified against Maven Central first.
- The bootstrap `.gitignore` contained the IDE pattern `out/`, which silently ignored the whole `adapter/out`
  package tree. Tests passed, commits looked fine, but two commits shipped without their adapter classes.
  It was caught by reading `git show --stat` before pushing; the unpushed commits were rebuilt correctly and
  the pattern anchored to `/out/`. Every later package addition was followed by `git status`.
- A Jackson 3 `MapperFeature` name was guessed from Jackson 2 and did not compile. The jar was inspected with
  `javap` and the feature that actually exists, `INFER_RECORD_GETTERS_FROM_COMPONENTS_ONLY`, turned out to be
  the better fit anyway.
- Gradle test suites do not inherit the main source set's compile classpath through `implementation(project())`;
  the integration suite failed to compile until the configurations were made to extend `implementation`.

## How correctness is verified

Unit tests on in-memory fakes, ArchUnit rules on every build, WireMock contract tests for the risk client,
Testcontainers on real Postgres and Redis for repositories, cache, outbox and the concurrency guard, Cucumber
scenarios over HTTP against the whole application, GitHub Actions on every push, and a manual run through
`compose.yaml`. The database enforces the append-only ledger with a trigger, so that invariant does not depend
on anyone, human or agent, remembering it.

## Agent tooling in this repository

- `CLAUDE.md` at the root and one per layer state what lives where, what is forbidden, and how each layer is
  tested. They are written for the next agent as much as for the next reader.
- `.claude/settings.json` allows the build and read-only git commands and denies history rewriting.
- `.claude/skills` vendors the complete seven-skill workflow bundle, documented in its
  [README](.claude/skills/README.md): project context initialization, implementation planning, an
  approval-gated implementation orchestrator, research spikes, systematic debugging, incoming change review
  and task closeout. Closeout was used in the original delivery: it triages what a finished task actually taught
  into a decision record, an instruction rule, agent memory or a work-log note, and discards the rest. The
  bundled snapshots document the repository workflow; later Codex work follows the current workflow available
  to that run.
- `context/map/**`, `.agents/project-context.md` files, `AGENTS.md` and the routing blocks in the `CLAUDE.md`
  files were generated by the project-context-initializer skill from the same toolkit after the code was
  finished. The run's coverage, snapshot fingerprints and per-lane verdicts are in `context/map/manifest.json`;
  it also surfaced the one contradiction between the documentation and the history (DECISIONS.md §16), which
  was fixed in the same commit.
- The idempotent settlement retries change (DECISIONS.md §17) was delivered end to end by that workflow:
  `implementation-planning` produced the plan under `context/plans/` (with one operator override recorded),
  `implementation-orchestrator` ran a worker and independent reviewers in isolated worktrees with SHA-bound
  gates, and `task-closeout` federated the results. The run caught two things worth knowing: a pre-existing
  cold-start flake in the acceptance suite, fixed by a repair packet instead of a rerun, and a self-contradictory
  committed run ledger, caught by the independent combined review and fixed before the merge. The whole trail is
  in `context/implementation-runs/`.
