# Execution profiles, orchestration modes and model policy

Choose the delivery contract immediately before product-code implementation. Keep four independent fields: execution profile, orchestration mode, terminal outcome and model policy.

## Mandatory pre-implementation decision

After plan/context/topology reconnaissance but before creating an implementation worktree or editing product code, present one compact recommendation and ask the operator to confirm or change:

1. **Execution profile:** `quick-dev`, `quick-bug-fix`, `standard` or `bug`.
2. **Orchestration mode:** `fire-and-forget` or `supervised`.
3. **Terminal outcome:** the exact state at which the run is complete.
4. **Model policy:** daily-coding defaults or exact per-role overrides.
5. **Automatic escalation:** whether a quick profile may become `standard`/`bug` without another routine confirmation when new evidence breaks quick eligibility.

The default orchestration recommendation is `fire-and-forget`. The default execution profile is `standard` unless repository evidence supports a quick profile. There is no universal default terminal outcome: it must match the request and authorization.

If the planner already asked this compound question and the operator answered it in the currently trusted conversation, consume that answer and do not ask a duplicate question. Restate the selected contract immediately before the first implementation side effect.

## Challenge contract

Do not agree reflexively with the requested profile, terminal outcome, model or release path.

When evidence supports a different choice:

1. state the concrete contradiction or risk;
2. recommend the safer/faster alternative and its cost;
3. identify which gate or assumption changes;
4. ask for the operator's decision once.

The operator has the final decision for product/process trade-offs within their authority. Record an override and its accepted risk, then execute it without repeatedly relitigating the same point. A decision cannot waive protected branches, required final checks, unavailable authority, law/policy or an unsafe destructive action.

Challenge both over-engineering and under-testing. A needlessly expensive `standard` run on a two-file isolated change deserves a `quick-dev` recommendation; a `quick-*` request touching authorization, migrations or cross-service contracts deserves a full-profile recommendation.

## Profile matrix

All profiles finish with the complete final gate chain on one assembled feature SHA. Profiles differ only in pre-edit proof and how often expensive gates run before final assembly.

| Profile | Required before edits | Per-slice cadence | Mandatory final feature gates |
| --- | --- | --- | --- |
| `quick-dev` | healthy baseline; focused expected behavior or focused RED where practical | focused tests plus cheap targeted build/type/static; Sonar, independent review and real-surface E2E may be `DEFERRED_TO_FINAL` | full tests/build/static, Sonar Quality Gate, independent combined review, full real-surface E2E, then required CI |
| `quick-bug-fix` | reproduce and prove the reported defect before any fix | focused regression test plus cheap targeted build/type/static; repeated heavy gates may be `DEFERRED_TO_FINAL` | full tests/build/static, Sonar Quality Gate, independent combined review, full real-surface E2E, then required CI |
| `standard` | healthy baseline and per-slice acceptance RED | focused tests, static, Sonar, independent review and real-surface E2E for every slice | repeat the complete combined gate chain and required CI |
| `bug` | reproduce every material defect path before any fix | the complete `standard` slice chain with reproduction/regression evidence | repeat the complete combined gate chain and required CI |

`DEFERRED_TO_FINAL` is not green. It is a named obligation that must pass on the final assembled feature SHA before a remote delivery step.

## `quick-dev`

Recommend only when evidence shows a small, targeted development change:

- one narrow capability or maintenance outcome;
- localized ownership, normally one module and a handful of first-party files (file count is a signal, not the decision);
- no schema migration/backfill/cutover;
- no new or changed public API/event/schema contract;
- no authorization, security, privacy, payment or permission boundary;
- no infrastructure/deployment topology change;
- no cross-service rollout or high-risk concurrency/data-integrity behavior;
- existing focused-test and final real-surface E2E paths are available;
- simple rollback and low blast radius.

Per slice, run focused tests and cheap targeted static/build checks. Do not spend time starting a separate real-surface E2E environment for every slice. Batch Sonar, independent combined review and real-surface E2E at final feature verification unless the change itself exposes a contract/security risk that makes earlier review necessary.

If the predicted surface grows, a risky boundary appears, or the final E2E cannot cover the behavior, escalate to `standard` before continuing under the reduced cadence.

## `quick-bug-fix`

Use for a narrow bug with low blast radius. It has the same small-surface eligibility as `quick-dev`, plus a hard rule: prove the bug exists before editing product code.

Capture:

- expected versus actual behavior;
- exact reproduction command/user actions;
- base SHA and environment;
- logs, screenshot/trace/output or failing test;
- explanation that distinguishes the bug from harness/environment failure.

Add the focused regression test and implement the root-cause fix. For multiple tiny repair packets, keep focused tests per packet and batch heavy gates at final feature verification. If reproduction fails, the root cause crosses boundaries, or the change expands materially, stop calling it quick and recommend `bug`.

## `standard`

This is the default execution profile. Preserve the full existing per-slice chain:

```text
acceptance RED
-> implementation
-> candidate SHA
-> focused tests/static
-> Sonar accounted
-> independent review
-> real-surface E2E
-> integration verification
```

Use for ordinary non-trivial features, multiple meaningful slices, changed contracts, migrations, cross-module behavior or any work that does not meet every quick eligibility condition.

## `bug`

Use the full `standard` chain, but require verified reproduction before product-code changes. Prefer this profile for:

- defects whose root cause is unknown or cross-cutting;
- concurrency, data-integrity, security, permission, payment or migration defects;
- production incidents or regressions spanning several modules/services;
- bugs that cannot be covered by one narrow repair and final E2E path.

Reproduction may use the real user surface, a focused integration test, trace/log evidence or a consumer-boundary test, but it must prove the reported failure rather than infer it from code.

## Orchestration modes

Both modes remain multi-agent. One orchestrator coordinates; implementation and repair stay in isolated worker worktrees; review remains independent.

### `fire-and-forget` — default

Use one up-front execution contract and continue autonomously until the terminal outcome or a true external/authority blocker.

- Do not pause for routine red gates, candidate commits, slice integrations, CI failures or authorized redeploys.
- Repair and re-run gates until the selected terminal outcome is proven.
- Run only dependency-ready agents and bound concurrency by verified worktree/resource independence.
- Keep packets small and avoid giving every worker the entire plan/context.
- Use condition-based waits for CI/deploy/runtime state rather than repeated narration or blind polling.
- Persist state/evidence so another orchestrator can resume without redoing valid work.
- Ask again only for new authority, a material scope/architecture decision, destructive/data action outside the envelope or an unavoidable external blocker.

Fire-and-forget does not mean “spawn everything and stop watching.” The orchestrator remains responsible for integration, failures, runtime proof and the final state.

### `supervised`

Use the same subagent/worktree architecture, but pause at operator-selected checkpoints. If the operator does not name checkpoints, use:

- after the first integrated slice or repair proof;
- before the first remote push/PR;
- before integration-branch merge;
- before staging promotion;
- before production merge/deployment.

Red technical gates still route to repair without asking whether to fix them. Checkpoints are for operator visibility and side-effect decisions, not for waiving quality.

## Terminal outcomes

Choose one exact outcome and include all required authority and verification:

- `local-green`: assembled local feature SHA passes all final local gates; no push.
- `ready-pr`: ready integration PR exists and all required PR checks on current head SHA are green.
- `integration-merged`: integration PR is merged and resulting SHA is verified; include any unavoidable automatic deployment effect in the authorization analysis.
- `staging-green`: expected SHA/artifact is deployed on staging and real-surface staging E2E is green.
- `production-pr-ready`: production-target PR/promotion request is ready with staging evidence; no production merge/deploy.
- `production-green`: explicitly authorized production merge/promotion/deploy completed, exact artifact is live, safe production verification passes and health/observability remain within the agreed acceptance window.

For `production-green`, define before implementation:

- exact production action and approver;
- rollback trigger and executable rollback path;
- safe smoke/E2E scope that will not corrupt production data;
- runtime revision/digest proof;
- health, error, latency/business signals and observation window;
- what constitutes automatic repair versus rollback versus operator escalation.

Never infer production authority from `fire-and-forget`, a plan status or permission to merge a non-production branch.

## Model policy

Model selection is configurable per role. The contract below is a resolution procedure, not a list of model names, because any list of model names in this file is stale the moment a provider ships.

Default policy: `daily-coding`.

### Resolving a role's model

1. Take the operator's explicit choice when one exists. It wins over everything here.
2. Otherwise ask the host which models it actually offers for this account, rather than assuming a name is available.
3. Pick the provider's current general-purpose coding tier — the one the provider positions for routine work, between its small and its frontier offering. Do not pick the frontier model by habit; do not pick the cheapest tier for work that needs reasoning.
4. Resolve any alias to a concrete model identifier before dispatch. An alias resolves differently by provider and by account, so an unresolved alias is not a recorded decision.
5. Record both the requested and the resolved identifier in the run ledger, per assignment.
6. When resolution fails, report the mismatch and stop rather than silently substituting. Use a fallback only when the operator allowed one.

### Role defaults

| Role | Tier | Default effort |
| --- | --- | --- |
| implementation/repair worker | current general-purpose coding tier | low for quick profiles; medium for full profiles |
| independent reviewer | the same tier, with an independent context | medium; raise only for demonstrated risk |
| orchestrator | current configured model unless overridden | enough to coordinate; do not use a frontier model by habit |

### Last verified model identifiers

These are a convenience, not the contract. Verify them against the host before use; when the verification date below is more than a few months old, treat every identifier here as unverified and resolve from the host instead.

- **Verified:** 2026-08-29
- **Codex general-purpose coding tier:** `gpt-5.6-terra`
- **Claude Code general-purpose coding tier:** `claude-sonnet-5`, or the `sonnet` alias once resolved to a concrete identifier

Sources: [OpenAI GPT-5.6 Terra](https://developers.openai.com/api/docs/models/gpt-5.6-terra), [Claude Code model configuration](https://code.claude.com/docs/en/model-config).

Rules:

- The operator may override any role's model or effort in the pre-implementation decision.
- Preserve an explicitly requested model. If unavailable, report the mismatch and use a fallback only when the operator allowed fallback.
- Prefer the daily-coding model for routine implementation. Escalate a worker/reviewer to a stronger model only for repeated root-cause failure, high-risk design/security/data reasoning or an explicit operator choice.
- Do not send the same full context to multiple models “for confidence.” Add an independent pass only when its decision value exceeds its token/time cost.

## Automatic profile escalation

In `fire-and-forget`, recommend `auto_escalation: allowed` so newly discovered risk does not require routine interaction.

Allowed transitions:

```text
quick-dev -> standard
quick-bug-fix -> bug
quick-bug-fix -> standard   # when reproduction disproves the bug framing but scoped feature work remains authorized
```

Escalation may add missing per-slice gates for all not-yet-integrated work and always records why. It never downgrades a profile automatically. A downgrade requires an operator decision.
