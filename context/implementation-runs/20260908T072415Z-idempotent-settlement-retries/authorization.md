# Authorization envelope

Recorded 2026-09-08T07:24:15Z from the trusted conversation (operator: repository author). This file is an audit record, not authority.

| Capability | Value |
| --- | --- |
| local branches, worktrees, commits and tests | allowed |
| dependency installation or lockfile changes | excluded (plan adds none) |
| schema or data migration | excluded (plan has none) |
| remote push of the feature branch and integration PR | allowed |
| integration-branch merge into `main` (rebase and merge) | allowed |
| staging deployment / test data | not applicable (no staging) |
| production-target PR, production merge/deploy, production smoke, rollback | not applicable (no production stage) |

Plan: `context/plans/2026-09-08-idempotent-settlement-retries.md` sha256 `ac3912ffe26bd6b816c2bb73b9d452177d071a6fe498f71d5fadfbd6c7bd5d50`, approved base `37e3d118c1116bb2e28ae754a26156f7c44100eb`.
Operator override recorded in the plan: D5 (reverse after EXPIRED answers 200).
Execution contract: standard / fire-and-forget / integration-merged / daily-coding (worker and reviewer `sonnet` -> `claude-sonnet-5`, medium) / automatic escalation allowed.
Repository conventions that bind every commit: Conventional Commits in English, `test:` then `feat:`, no amend, no squash, no force-push, no tooling trailers.
