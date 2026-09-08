# Scheduler

Single slice S1; no parallel cohort. Order: S1 (worker) -> static -> Sonar NOT_APPLICABLE -> independent review -> E2E -> integrate into feature worktree -> feature-head `./gradlew build` -> reconciliation commit -> final gates on assembled SHA (full build, combined review, full acceptance suite) -> push -> PR -> CI -> rebase-merge -> verify `main`.
