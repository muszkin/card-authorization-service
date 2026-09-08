# Quality-gate contract

Every gate result is bound to exact evidence. Use repository-defined commands; never invent a generic replacement that tests a different surface.

## Result model

Allowed statuses:

- `NOT_RUN`
- `RUNNING`
- `PASS`
- `FAIL`
- `NOT_APPLICABLE`
- `DEFERRED_TO_FINAL`
- `DEFERRED_TO_CI`
- `BLOCKED_EXTERNAL`
- `STALE`

Only `PASS` satisfies a required locally available gate. `NOT_APPLICABLE` requires evidence that the repository does not define that capability. `DEFERRED_TO_FINAL` is allowed only for the per-slice heavy gates named by `quick-dev` or `quick-bug-fix`; it means the obligation will run against the assembled feature SHA before remote delivery and is never green. `DEFERRED_TO_CI` is allowed to progress toward a PR only when required CI is the repository's sole supported execution path; it remains an open obligation that must pass on the current PR SHA. `BLOCKED_EXTERNAL` is not green.

Every result records:

- gate and scope;
- commit SHA;
- exact command and working directory;
- start/end time and exit code;
- tool/runtime version;
- evidence path or external check URL;
- status and concise interpretation;
- environment/resource lease;
- baseline comparison when relevant.

## Profile cadence

The selected profile controls when a gate runs, not what constitutes success:

- `standard` and `bug` run the full ordered gate chain for each slice and again for the assembled feature;
- `quick-dev` and `quick-bug-fix` run focused tests plus cheap repository build/type/static checks for each slice and may mark per-slice Sonar, independent review and real-surface E2E `DEFERRED_TO_FINAL`;
- all profiles run fresh full static analysis, the repository's complete test set, Sonar disposition and actual Quality Gate, independent combined review, and full real-surface E2E against one immutable assembled feature SHA;
- CI-only capabilities use `DEFERRED_TO_CI`, not `DEFERRED_TO_FINAL`.

A quick slice with open `DEFERRED_TO_FINAL` gates is eligible only for integration under the quick contract; it is not independently release-ready. Every deferred record names the final gate, scope, required feature state and invalidation rule.

## Focused tests and build checks

Run the smallest truthful unit, component, integration, compile, type or build checks that cover the slice. Include contract tests for changed APIs, events, schemas or shared packages.

Focused checks supplement E2E; they do not replace it.

## Static analysis

Discover commands from package scripts, build files, CI, repository instructions and project context. Static analysis may include:

- compiler/type checks;
- lint and formatting checks;
- language-specific analyzers;
- dependency/boundary checks;
- security/static scanners configured as repository gates.

Do not auto-fix unrelated files. If a formatter changes files, inspect and include only intentional results, then invalidate downstream gates.

## Dependency, secret and license scanning

Sonar and lint do not cover supply chain or leaked credentials. Treat this as a named part of static analysis with its own evidence.

1. Detect the repository's configured scanners: dependency advisory checks, secret scanners, software bill-of-materials tooling and license checks, in repository configuration, hooks or required CI. Run the configured command and read its actual result, not its exit code alone.
2. Two obligations hold in every profile even when the repository configures no scanner at all, because they are checks on the change itself rather than on the project:
   - **Introduced secrets.** Inspect the candidate diff for credentials, tokens, private keys, connection strings and personal machine paths. Any hit is a blocking finding, and a secret that reached a pushed commit additionally requires rotation, which is an operator decision, not a cleanup commit.
   - **Introduced dependencies.** For every dependency the change adds or upgrades, including transitive movement visible in the lockfile, record the resolved version, its advisory status and its license, and whether it is compatible with the repository's constraints.
3. Distinguish pre-existing advisories in untouched dependencies from advisories the change introduces. A repository-required overall gate is not waived by age, but a baseline advisory is classified rather than blamed on the slice.
4. Use `NOT_APPLICABLE` only with evidence that the repository defines no such capability and the change adds no dependency. Unavailable tooling with a required gate is `BLOCKED_EXTERNAL`.

Quick profiles run the diff-level secret and introduced-dependency checks per slice, because they are cheap and their cost rises sharply after a push. A full repository scan follows the same cadence as the rest of static analysis and must close on the assembled feature SHA.

## Sonar

Sonar is distinct from local static analysis.

The gate is the platform's server-side quality verdict for the analyzed revision, not the vendor. `Sonar` is the name used throughout these skills because it is what these repositories run; a repository using a different code-quality platform substitutes it wherever `Sonar` appears and keeps the contract unchanged. What must not change is the meaning: a scanner exit code is not the verdict, and the verdict belongs to the analyzed SHA.

1. Detect whether SonarQube or SonarCloud is configured in repository files or required CI.
2. Use the repository's scanner command and project configuration.
3. Give concurrent worktrees unique analysis branch/PR keys and report directories.
4. Wait for and read the actual server-side Quality Gate for the analyzed SHA/new-code period.
5. Preserve dashboard/task URL, analysis ID and failed condition details.

Do not call scanner exit code zero a green Quality Gate. If Sonar exists only in CI, mark the slice and combined local result `DEFERRED_TO_CI`, register the named required check, and close the obligation only when that check passes on the current PR head SHA. If a supported local Sonar path is required but credentials/server access are unavailable, use `BLOCKED_EXTERNAL`, not `DEFERRED_TO_CI` or `NOT_APPLICABLE`.

Distinguish pre-existing baseline debt from new-code failures, but do not waive a repository-required overall Quality Gate.

## Independent review

Review must use the exact immutable diff and the rubric in `review-rubric.md`. Any unresolved material finding makes the review gate `FAIL`.

The reviewer may produce `PASS`, `FAIL` or `BLOCKED_EXTERNAL`; silence or a summary without inspected evidence is not a pass.

## Real-surface E2E

Choose the closest available user surface:

1. browser/desktop/mobile UI interaction;
2. terminal interaction for a CLI/TUI;
3. protocol/API calls only when the product itself is an API or no higher user surface exists.

For UI products, use headed local E2E when supported by repository instructions. Direct requests may create fixtures or inspect diagnostics, but may not replace clicking/typing through the tested journey.

Record:

- controlled initial state and fixture identity;
- exact user actions and observable outcomes;
- screenshots/traces/video/logs where supported;
- cleanup or retained test data;
- candidate SHA and running application SHA;
- browser/device/runtime version.

An E2E pass against a different build or stale server is invalid.

## CI

Bind CI to the current PR head SHA. Required checks include branch protection, merge queue and repository-documented gates. A check from an older SHA becomes `STALE` after a push.

For a red check:

1. obtain the failing job logs and artifacts;
2. classify the failure;
3. reproduce locally where possible;
4. repair in an isolated worktree;
5. rerun affected local gates;
6. push and wait for checks on the new SHA.

Do not rerun flaky CI blindly without capturing why a rerun is justified.

## Staging verification

A green deployment job is not enough. Prove:

- the integration commit or expected artifact digest was built;
- that exact revision is running on staging;
- health/rollout completed;
- migrations and asynchronous consumers are compatible;
- real-surface staging E2E passes against that revision.

Keep production actions outside this gate unless the confirmed terminal outcome is `production-green`; even then, staging proof remains a separate prerequisite when staging exists.

## Production verification

For an authorized `production-green` outcome, bind proof to the exact promoted SHA or artifact digest. Run only the production smoke/E2E actions explicitly classified as safe, capture the agreed health signals for the recorded observation window, and apply the recorded repair-versus-rollback rule on failure. A green deploy workflow without runtime revision proof and observed health is not `PRODUCTION_GREEN`.
