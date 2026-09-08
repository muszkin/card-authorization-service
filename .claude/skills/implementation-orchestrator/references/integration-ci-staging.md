# Integration, CI and staging delivery

The orchestrator integrates eligible slices, proves the assembled feature, follows the repository's real release topology and stops exactly at the selected terminal outcome.

## Integrating a slice

Acquire the single integration lock and verify:

- worker head and evidence SHAs match;
- the slice is either fully gate-green under `standard`/`bug` or explicitly `QUICK_READY_FOR_INTEGRATION` with complete `DEFERRED_TO_FINAL` obligations under a quick profile;
- slice worktree is clean except intentional committed changes;
- feature HEAD is green;
- dependencies are integrated;
- no conflicting active slice owns the same contract or path.

Update the slice against current feature HEAD using repository policy. The orchestrator may perform only a conflict-free mechanical update. Any conflict resolution, generated-output refresh or other product/test content edit goes back to the logical owner with a focused update packet; mark affected gates stale and send the result through the required gate chain.

Use squash integration when that is the repository/default agreed workflow, preserving traceability from run slice ID to integrated commit. Do not merge unreviewed conflict resolutions.

## Feature-head verification after every merge

Before starting the feature application or tests, acquire and bind dedicated integration-verification leases for every applicable resource in [resource-isolation.md](resource-isolation.md). Use isolated resources or wait for conflicting worker leases to be verified released; a different worktree does not make shared runtime safe. Record the effective bound resources.

Immediately run the plan's integration checks at the new feature SHA:

- tests for the integrated behavior;
- existing behavior most likely to regress;
- changed API/event/schema consumers;
- migration and mixed-version compatibility where applicable;
- the combined real-surface journey when affordable.

If red:

1. preserve evidence and classify ownership;
2. mark feature HEAD red and stop dependent integration;
3. create a fresh repair branch/worktree from current feature HEAD;
4. give the repair to the same logical owner or narrowest boundary owner;
5. run the invalidated loop required by the active profile, escalating a quick profile when its eligibility no longer holds;
6. integrate and repeat feature-head verification.

Do not continue stacking slices onto a red feature branch.

After evidence is retained, tear down only integration-owned processes/fixtures and release the verification leases. On failure, retain the lease only when required to preserve evidence, record why, and prevent a conflicting assignment.

## Final feature gates

After all slices, reconcile tracked architecture/ADR/directory context and useful implementation evidence first. Commit any intended tracked reconciliation changes, freeze the candidate SHA, then acquire/bind a dedicated final-verification resource lease. Isolate it from or serialize it against all remaining worker/integration leases, prove the effective resources, close every `DEFERRED_TO_FINAL` obligation, and for every profile run:

1. full build/type/lint/static analysis;
2. full repository-defined tests;
3. Sonar analysis and actual Quality Gate, or a named `DEFERRED_TO_CI` obligation only when required CI is the sole supported Sonar path;
4. fresh independent combined review of the entire feature delta;
5. full local real-surface E2E, headed where supported.

Bind every pass to the same immutable feature SHA. A repair or later tracked context/documentation change invalidates the relevant chain and produces a new candidate SHA. A CI-only Sonar obligation means the feature is `FEATURE_READY_FOR_PR`, not Sonar-green.

Retain final evidence, then tear down and release the final-verification lease before delivery unless the repository requires the same controlled runtime to remain available; record any retained ownership explicitly.

## Integration PR

Open a ready PR from the feature branch to the detected integration target only when the selected terminal outcome reaches remote delivery, local feature gates pass, every `DEFERRED_TO_FINAL` obligation is closed, and any CI-only gates are explicitly registered. The PR is the execution surface for CI-only obligations; do not call them passed before CI reports on the current head SHA.

The PR records:

- approved plan and implemented slices;
- user-visible happy paths and edge cases;
- architecture/ADR and contract changes;
- migration/rollout order;
- exact local gate results and evidence;
- known baseline issues and remaining risks;
- staging verification plan;
- no claim of production deployment.

Respect repository PR templates and do not add agent/AI co-authorship metadata.

## Required CI loop

For the current PR head SHA:

```text
WAIT_REQUIRED_CHECKS
  -> RED: COLLECT_EVIDENCE -> REPAIR_WORKTREE -> LOCAL_GATES -> PUSH -> WAIT_REQUIRED_CHECKS
  -> GREEN: MERGE_ELIGIBLE
```

Inspect logs and artifacts, not only check names. If CI exposes a failure unavailable locally, preserve it as the repair reproduction evidence and run all locally available affected gates before pushing.

After each push, discard earlier CI conclusions as stale. Do not use a retry quota to abandon an in-scope fix.

## Integration merge

Merge the integration PR only when:

- the authorization envelope includes it;
- required CI and reviews are green on current SHA;
- mergeability and branch protections are satisfied;
- the staging/release topology is still understood;
- the merge does not itself constitute unauthorized production deployment.

Use the repository's configured merge strategy. Record resulting merge/squash SHA.

## Staging deployment proof

If staging exists, determine whether deployment is automatic after merge or requires an authorized action. Verify:

- deployment run completed;
- expected integration SHA produced the artifact;
- artifact digest/version deployed equals the expected one;
- rollout and health checks completed;
- migrations and background consumers are healthy;
- staging reports the expected revision when observable.

Do not equate “workflow green” with “runtime updated.”

### Staging revision mismatch loop

If observed staging SHA/version/digest differs from the expected artifact, enter:

```text
STAGING_REVISION_MISMATCH
  -> COLLECT_BUILD_AND_DEPLOY_PROVENANCE
  -> AUTHORIZED_REDEPLOY | PIPELINE_REPAIR_PR
  -> WAIT_ROLLOUT
  -> VERIFY_EXPECTED_REVISION
```

Trace source SHA -> build run -> artifact digest/version -> deployment input -> runtime-observed revision. If the correct artifact exists and the authorization envelope covers a staging redeploy, redeploy through the normal mechanism. If build/deploy selection is defective, route a pipeline repair through the normal worktree, static/Sonar/review/test, PR and CI gates. Re-check the observed runtime revision after rollout.

Do not run evidentiary staging E2E or open the production-target PR while the revision is mismatched. A smoke against the wrong artifact may help diagnosis but cannot satisfy acceptance.

## Staging E2E loop

Only after the expected SHA/artifact digest is observed on staging, run the planned happy paths, critical edge cases and regression journeys through the real staging user surface.

If red:

1. capture browser/device/terminal evidence and staging revision;
2. determine product vs data/environment failure;
3. if product code is responsible, create a repair branch from the proper current integration base;
4. pass the repair through static, Sonar, independent review, E2E, PR and required CI;
5. merge under the same authority, redeploy and prove the new revision;
6. repeat staging E2E.

Do not patch staging manually unless the user explicitly authorized and repository policy defines that route.

## Production-target PR

After staging is green, open a ready PR or promotion request to the repository's actual production-deployment target. Include:

- integration and staging SHAs/artifact digest;
- staging deployment proof;
- staging E2E evidence;
- migrations, flags, compatibility and rollback notes;
- known risks and required production monitoring;
- explicit statement that production merge/deployment has not occurred.

Creating this PR proves `production-pr-ready`. Stop here unless the confirmed terminal outcome is `production-green` and its explicit production authorization/verification contract is complete.

## Production-green loop

For an explicitly authorized `production-green` outcome:

1. verify the production PR/promotion object, required checks, approvals, mergeability and current expected artifact;
2. reconfirm the recorded production merge/deploy, smoke/E2E and rollback capabilities at the action boundary;
3. merge or promote only through repository controls;
4. trace source SHA -> build -> immutable artifact digest/version -> production deployment input -> runtime-observed revision;
5. do not run acceptance verification until the expected revision is observed;
6. run only the recorded safe production smoke/E2E journeys and capture user-visible evidence;
7. observe the agreed health/error/business signals for the recorded window;
8. if verification or health is red, apply the pre-recorded repair-versus-rollback rule. A repair returns through worktree, static, Sonar, independent review, E2E, PR/CI and promotion; a rollback uses only the authorized normal mechanism and is followed by revision and health proof;
9. declare `PRODUCTION_GREEN` only when artifact identity, smoke/E2E and observation all pass.

A production workflow marked successful is not enough. If rollback authority, safe verification or runtime provenance is missing, stop at the highest earlier proved outcome and report the exact authority/evidence blocker.

## Terminal outcomes

- `local-green`: assembled feature passes the complete local final chain; no push.
- `ready-pr`: ready integration PR exists and required CI on its current head is green.
- `integration-merged`: the authorized integration merge is recorded; do not infer staging.
- `staging-green`: exact expected staging revision is running and staging E2E is green.
- `production-pr-ready`: production promotion PR/request is ready after verified staging or the repository-equivalent pre-production proof.
- `production-green`: exact production artifact is live, safe production verification is green, and the observation window passes.

Do not execute stages after the selected outcome. If repository topology collapses stages, map the selected semantic outcome to actual side effects and keep any production effect behind explicit production authority.

## Repositories without a separate staging or production branch

Model the actual topology instead of inventing branches:

- if staging is an environment promotion of one artifact, verify the promotion and open the repository's production approval object;
- if there is no staging, record `NOT_APPLICABLE` with repository evidence and stop at the safest pre-production handoff;
- if the integration branch is also production-deploying, keep its merge outside the default envelope unless the user explicitly authorized that production effect.

The skill's persistence applies inside authorized boundaries; it never justifies bypassing release controls.
