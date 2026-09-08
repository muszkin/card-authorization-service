# Research report

One artifact per question. Follow the repository's existing research or decision convention when it has one; otherwise propose `docs/research/YYYY-MM-DD-<topic>.md` and say that the path is a proposal.

Do not leave placeholders. Omit a section that does not apply rather than filling it with generic text.

```markdown
# Research: <question>

**Status:** draft | ready-for-approval
**Date:** YYYY-MM-DD
**Decision unblocked:** <what cannot proceed until this is answered>
**Decision owner:** <who decides>
**Budget spent:** <time, and whether the budget was exhausted>

## Question

<One falsifiable question. The success and kill criteria that were declared before any spike.>

## Constraints this answer must satisfy

<The project's actual runtime, versions, deployment target, data shape, load, operational limits, licensing, and security constraints. Cite where each comes from.>

## Prior art

| Source | What it establishes | Provenance |
| --- | --- | --- |
| `<repository path, ADR, prior research, or external report>` | <fact> | Observed / User-confirmed / Inferred / Unknown / Contradiction |

<Include what the repository already tried and why it was abandoned, when that exists.>

## Options

<The incumbent is always one row. "Do nothing" and "extend what exists" are legitimate incumbents.>

| Option | Current version and release date | License | Operational surface added | Migration and exit cost | Source and retrieval date |
| --- | --- | --- | --- | --- | --- |

### Trade-offs

<Per option: what it makes easier, what it makes harder, and the conditions under which it is the wrong choice. Name the failure mode, not just the benefit.>

## Evidence

| Claim | Source | Retrieved | Provenance |
| --- | --- | --- | --- |
| <version, limit, compatibility, or behavior> | <primary source URL or repository path> | YYYY-MM-DD | Observed / User-confirmed / Inferred / Unknown |

<Every consequential claim appears here. A claim with no row was not verified and must be marked Unknown in the verdict.>

## Spike

<Omit this section when the question was settled by reading, and say so in the verdict.>

- **Why reading was insufficient:** <the decision-relevant gap>
- **Kill criteria declared up front:** <exact observations>
- **Environment:** <worktree, versions, dataset, isolation>
- **Method:** <what was built, what was measured, how, and what was held constant>
- **Result:** <raw observations, including the distribution for any measurement>
- **Disproved:** <what the spike ruled out; this is often the most valuable part>
- **Teardown:** <worktree removed, resources released, checkout unchanged>

## Verdict

**Verdict:** adopt | adopt-with-constraints | reject | needs-more-evidence

<One paragraph stating the verdict and the single strongest piece of evidence behind it.>

### Constraints on adoption

<For adopt-with-constraints: each boundary, and the evidence that draws it.>

### Why the alternatives lost

| Option | Why it lost |
| --- | --- |

### Residual unknowns

| Unknown | Impact if wrong | How it would be closed | Owner |
| --- | --- | --- | --- |

## Handoff

- **Consumer:** implementation-planning
- **What planning still owns:** version pinning, vertical slicing, gate contract, rollout and rollback
- **Federation record:** <path, content hash, source revision, consumers>
- **This artifact authorizes nothing.**
```

## Report discipline

State the verdict in the first screen. A reader deciding whether to open the full report should not have to scroll to learn the answer.

Keep the losing options in the artifact. A report that documents only the winner cannot be re-evaluated when the constraints change, and the next agent will redo the same survey.

When a claim could not be verified, say so in the verdict rather than only in the evidence table. An unverified version claim that reaches a plan becomes a pinned dependency.
