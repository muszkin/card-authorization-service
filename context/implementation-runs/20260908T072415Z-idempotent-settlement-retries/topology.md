# Release topology

- Default branch and integration target: `main` (only branch; 0 pull requests so far; linear history).
- Feature branch: `feat/idempotent-settlement-retries` from `37e3d118c1116bb2e28ae754a26156f7c44100eb`; slice branch `feat/idempotent-settlement-retries-s1`.
- Merge policy: GitHub PR, **rebase and merge** (keeps `test:` -> `feat:` commits; no merge commits on `main`).
- Required checks: none enforced by branch protection (GET protection -> 404); workflow `build` runs on `pull_request` and on push to `main` and is treated as required by this run.
- Staging / production: none defined in the repository (README, context map) -> NOT_APPLICABLE.
