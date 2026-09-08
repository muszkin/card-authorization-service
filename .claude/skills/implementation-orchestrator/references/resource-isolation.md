# Runtime resource isolation

Separate worktrees do not isolate runtime. Two assignments in two directories still share the same ports, the same database, the same browser profile and the same remote namespaces unless something allocates them deliberately.

This is the single canonical list. Every skill that runs code concurrently — slice workers, integration verification, final feature verification, a research spike, a defect reproduction — leases from it rather than restating its own version.

## The resources

Allocate a unique value for each one that applies:

- application and test ports;
- database instance, database name or schema;
- container/project namespace;
- queues, topics, buckets or emulator namespace;
- cache and temporary directories;
- browser profile and download directory;
- mobile simulator/device;
- test accounts, tenants and fixtures;
- code-quality analysis key and scanner report directory, such as a Sonar branch or PR key.

Serialize work that cannot safely isolate a shared resource. An unsafe shared database, device or remote namespace gets one owner at a time, and the lease transfers only after verified teardown.

## Lease states

Track every lease through:

```text
REQUESTED -> ACQUIRED -> BOUND -> RELEASING -> RELEASED
```

`STALE` is the reconciliation state for a lease whose recorded value no longer matches reality.

1. The coordinator probes and reserves the concrete resource before dispatch.
2. The assignee binds its actual process or configuration to the assigned value and reports proof.
3. Tests record the effective bound resource, not only the intended environment variable. A port in a configuration file that the process ignored is not evidence.
4. On completion, interruption or reassignment, stop owned processes and containers, clean only owned fixtures, verify teardown, then release.
5. On resume, compare the ledger against actual ports, processes, containers, databases and browser profiles before reusing or reallocating anything.

Do not release a lease merely because an agent turn ended. An abandoned lease that is silently reused produces failures that look like flakiness and cost far more to diagnose than they cost to prevent.

## Proving independence before parallel work

Two assignments may run concurrently only when every applicable resource above is either uniquely allocated to each of them or provably unused by both. A shared resource that "usually works" is a serialization requirement, not an isolation strategy.

Record the effective bound resources per assignment. When a failure is later classified as `flaky-or-environmental`, this ledger is the first evidence to check.

## Outside an orchestrated run

A disposable spike and a defect reproduction lease from the same list. They typically need fewer entries, but the ones they do need are leased and released the same way, and teardown is reported rather than assumed.

When the host cannot isolate a required resource at all, that is `BLOCKED_EXTERNAL`. Do not fall back to sharing it and hoping the timing works out.
