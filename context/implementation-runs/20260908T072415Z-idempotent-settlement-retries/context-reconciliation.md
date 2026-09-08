# Context reconciliation

| Artifact | Producer | Path | sha256 | Source revision | Approval | Consumers |
| --- | --- | --- | --- | --- | --- | --- |
| implementation plan | implementation-planning | `context/plans/2026-09-08-idempotent-settlement-retries.md` | `ac3912ffe26bd6b816c2bb73b9d452177d071a6fe498f71d5fadfbd6c7bd5d50` | `37e3d118c1116bb2e28ae754a26156f7c44100eb` | approved 2026-09-08 (operator) | implementation-orchestrator, review, task-closeout |
| run ledger | implementation-orchestrator | `context/implementation-runs/20260908T072415Z-idempotent-settlement-retries/` | (mutable until TARGET_REACHED) | `37e3d118c1116bb2e28ae754a26156f7c44100eb` | n/a | task-closeout, project-context-initializer refresh |

To federate at closeout: add both to `context/map/INDEX.md` and `manifest.json` related artifacts; refresh the application scoped context (new invariant: settlement replay after the lock).
