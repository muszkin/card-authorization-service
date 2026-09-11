<!-- BEGIN project-context-initializer:artifact -->
# card-authorization-service — project context index

- Scope: repository (`.`), single Gradle module. Operation: `refresh`. Classification: `brownfield`.
- Affected source: `5debbf647734dadcae75960f405ba5602a9bc82b` on `fix/safe-numeric-boundaries`; refreshed 2026-09-11T09:42:24Z.
- Coverage: complete (the numeric-boundary source, adjacent API/E2E surface, plan and frozen run checkpoint were rescanned). Freshness: partial: the source revision is fixed, but generated checkpoint and map files make the current worktree snapshot non-comparable.

Current user instructions, the code, its tests, runtime behaviour and the canonical documents (`README.md`,
`DECISIONS.md`, `AI_USAGE.md`, `CLAUDE.md`) outrank this generated map. Verify stale or high-risk claims at the
source.

## Read next

| Consumer | Read next |
| --- | --- |
| research | [project-overview.md](project-overview.md), [documentation-index.md](documentation-index.md), the scoped context of the area, [risks-and-unknowns.md](risks-and-unknowns.md) |
| implementation planning | overview, [technology.md](technology.md), [architecture-and-flows.md](architecture-and-flows.md), [dependencies.md](dependencies.md), [delivery-and-verification.md](delivery-and-verification.md), [git-and-pr-history.md](git-and-pr-history.md) hotspots, risks, scoped contexts |
| review | architecture, dependencies (contracts), risks, Git co-change, delivery gates, `DECISIONS.md`, scoped contexts |
| implementation orchestration | approved plan, frozen prior-run checkpoint, overview, architecture and `DECISIONS.md`, dependencies, delivery commands and gates, risks, nearest scoped contexts |
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
- Approved numeric-boundary plan: [safe numeric boundaries](../plans/2026-09-11-safe-numeric-boundaries.md).
- Frozen numeric-boundary checkpoint: [RUN.md](../implementation-runs/20260911-safe-numeric-boundaries/RUN.md) (120 test, 20 integration and 12 HTTP acceptance scenarios passed before final assembled delivery gates; its pending final gates are historical).

## Manifest

[manifest.json](manifest.json) lists every directory as `own`, `rolled-up` or `excluded`, the source snapshot,
fingerprints, per-lane verdicts and next actions. Use it for rolled-up and excluded directories.

## Unresolved contradictions

None. The one found during initialization (DECISIONS.md §16 versus the amended final commit) is resolved in the
commit that adds this map; see risks-and-unknowns.md item 1.

Freshness rule: verify high-risk claims against current source and the delivery PR. The frozen checkpoint deliberately
precedes final SHA-bound gates; its recorded authorization is historical evidence, never current authority.

<!-- END project-context-initializer:artifact -->
