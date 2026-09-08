# Integration history

| # | Slice | Candidate | Method | Feature head after | Feature-head check |
| --- | --- | --- | --- | --- | --- |
| 1 | S1 | `ddd71fada0e301552715ec135e4e3366dd328068` (parent `f0f62f4`, base `37e3d11`) | fast-forward of `feat/idempotent-settlement-retries-s1` into `feat/idempotent-settlement-retries` at 2026-09-08T07:40:17Z | `ddd71fa` | `./gradlew build`: test 106/0, integrationTest 20/0, acceptanceTest 10/0 (`feature-head-build.txt`) |

Reconciliation commit (plan, run ledger, DECISIONS §17, README, application context, map refresh) follows on the feature branch; final gates run on that SHA.

| 2 | R1 (repair: acceptance harness warm-up) | `80528ab665a5ce44294b69bd90adb60a0051be58` (parent `3ddbb48`) | fast-forward of `feat/idempotent-settlement-retries-r1` at 2026-09-08T08:01:09Z | `80528ab` | full build on the next reconciliation SHA (see `combined-gates.md`) |
