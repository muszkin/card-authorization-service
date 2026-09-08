# Resource leases

| Lease | Resource | Holder | State |
| --- | --- | --- | --- |
| L1 | slice worktree `/home/muszkin/work/zilch/worktrees/s1` (own `build/`), Testcontainers session of its JVMs, random ports | worker-S1 | ACQUIRED |
| L2 | feature worktree `/home/muszkin/work/zilch/worktrees/feature` (own `build/`) | orchestrator (integration/final verification) | ACQUIRED, serialized after L1 |
| L3 | review worktree `/home/muszkin/work/zilch/worktrees/review-s1` (detached at `ddd71fa`, read-only) | reviewer-S1 | ACQUIRED |

Shared: Docker daemon and Gradle daemon (safe: no fixed ports, no compose in tests, work is sequential).
