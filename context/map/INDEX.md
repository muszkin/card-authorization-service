<!-- BEGIN project-context-initializer:artifact -->
# card-authorization-service — project context index

- Scope: repository (`.`), single Gradle module. Operation: `initialize`. Classification: `brownfield`.
- Source: `b0e9abca8593fdb020a383837cb03433dd318691` on `feat/idempotent-settlement-retries`, clean worktree. Initialized 2026-09-07T14:57:44Z, refreshed 2026-09-08T07:41:14Z.
- Coverage: complete (every tracked directory accounted for; generated planning and run artifacts roll up into the root context). Freshness: complete for this snapshot; regenerate
  after any change to instructions, build files, migrations or package layout.

Current user instructions, the code, its tests, runtime behaviour and the canonical documents (`README.md`,
`DECISIONS.md`, `AI_USAGE.md`, `CLAUDE.md`) outrank this generated map. Verify stale or high-risk claims at the
source.

## Read next

| Consumer | Read next |
| --- | --- |
| research | [project-overview.md](project-overview.md), [documentation-index.md](documentation-index.md), the scoped context of the area, [risks-and-unknowns.md](risks-and-unknowns.md) |
| implementation planning | overview, [technology.md](technology.md), [architecture-and-flows.md](architecture-and-flows.md), [dependencies.md](dependencies.md), [delivery-and-verification.md](delivery-and-verification.md), [git-and-pr-history.md](git-and-pr-history.md) hotspots, risks, scoped contexts |
| review | architecture, dependencies (contracts), risks, Git co-change, delivery gates, `DECISIONS.md`, scoped contexts |
| implementation orchestration | an approved plan (none exists yet), overview, architecture and `DECISIONS.md`, dependencies, delivery commands and gates, risks, nearest scoped contexts |
| implementer | the approved plan, the scoped context you are working in, dependencies, delivery commands, invariants listed in each context |

## Areas

| Area | Path | Scoped context |
| --- | --- | --- |
| repository root: build, config, CI, local run, test suites, agent tooling | `.` | [.agents/project-context.md](../../.agents/project-context.md) |
| domain | `src/main/java/pl/fairydeck/authorization/domain` | [project-context.md](../../src/main/java/pl/fairydeck/authorization/domain/.agents/project-context.md) |
| application | `src/main/java/pl/fairydeck/authorization/application` | [project-context.md](../../src/main/java/pl/fairydeck/authorization/application/.agents/project-context.md) |
| adapter (rest, persistence, cache, risk, events) | `src/main/java/pl/fairydeck/authorization/adapter` | [project-context.md](../../src/main/java/pl/fairydeck/authorization/adapter/.agents/project-context.md) |
| technical (logbook, httpclient) | `src/main/java/pl/fairydeck/authorization/technical` | [project-context.md](../../src/main/java/pl/fairydeck/authorization/technical/.agents/project-context.md) |
| acceptance suite (Cucumber over HTTP) | `src/acceptanceTest` | [project-context.md](../../src/acceptanceTest/.agents/project-context.md) |

## Central artifacts

[project-overview.md](project-overview.md) · [technology.md](technology.md) · [architecture-and-flows.md](architecture-and-flows.md) ·
[dependencies.md](dependencies.md) · [documentation-index.md](documentation-index.md) · [delivery-and-verification.md](delivery-and-verification.md) ·
[git-and-pr-history.md](git-and-pr-history.md) · [risks-and-unknowns.md](risks-and-unknowns.md) ·
diagrams: [module-dependencies.mmd](diagrams/module-dependencies.mmd), [primary-runtime-flow.mmd](diagrams/primary-runtime-flow.mmd)

Canonical project documents: [README.md](../../README.md), [DECISIONS.md](../../DECISIONS.md),
[AI_USAGE.md](../../AI_USAGE.md), [CLAUDE.md](../../CLAUDE.md).

## Related artifacts

- Plan: [2026-09-08 idempotent settlement retries](../plans/2026-09-08-idempotent-settlement-retries.md) (approved).
- Implementation run: [RUN.md](../implementation-runs/20260908T072415Z-idempotent-settlement-retries/RUN.md) (gates, independent review, E2E evidence, integration history).
- Decision record: [DECISIONS.md §17](../../DECISIONS.md).

## Manifest

[manifest.json](manifest.json) lists every directory as `own`, `rolled-up` or `excluded`, the source snapshot,
fingerprints, per-lane verdicts and next actions. Use it for rolled-up and excluded directories.

## Unresolved contradictions

None. The one found during initialization (DECISIONS.md §16 versus the amended final commit) is resolved in the
commit that adds this map; see risks-and-unknowns.md item 1.

Freshness rule: this map describes `b4bef16`. If `git status` is not clean or HEAD differs, treat every claim as
possibly stale and refresh.
<!-- END project-context-initializer:artifact -->
