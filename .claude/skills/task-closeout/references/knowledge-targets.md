# Knowledge targets

Each disposition has exactly one target, one write contract, and one authorization level. Resolve the target from the project's actual conventions before falling back to the defaults here.

## Target resolution

Discover the convention before inventing one:

1. Read the instruction files and the resolved context index for an existing decision-record, changelog, or work-log convention.
2. Inspect the repository for `docs/adr/`, `docs/decisions/`, `doc/architecture/decisions/`, or an equivalent directory with existing records. Match its numbering, front matter, and file naming exactly.
3. Only when no convention exists, propose the defaults below and say that they are proposals.

Never create a second convention beside an existing one. A repository with two decision-record directories has no decision records.

## `emit-adr` — repository decision record

Default path: `docs/adr/NNNN-<kebab-title>.md`, continuing the existing sequence.

Authorization: repository-local write, covered by invoking this skill. Committing is not.

```markdown
# NNNN. <Decision in one line>

**Status:** proposed | accepted | superseded by NNNN
**Date:** YYYY-MM-DD
**Deciders:** <who actually decided>

## Context

<The forces that made a decision necessary. Cite the evidence: paths, runs, measurements, external sources with retrieval dates.>

## Decision

<What was decided, in the imperative.>

## Alternatives considered

| Alternative | Why it lost |
| --- | --- |

## Consequences

<What becomes easier, what becomes harder, and what is now accepted as a risk with its owner.>

## Revisit when

<The concrete condition that would reopen this decision.>
```

An ADR supersedes rather than edits. When a new decision replaces an old one, mark the old record superseded and link both directions.

## `emit-instruction-rule` — agent instruction files

Targets: the repository's own instruction files, and the source of the globally installed rules when the rule is not repository-specific.

Authorization: repository-local write covered by invoking this skill. A rule that would apply to every repository is proposed, never written directly.

Respect block ownership before writing anything. An instruction file may contain a sentinel-delimited block owned by a generator or an installer:

- **Never write inside a block owned by something else.** That content is regenerated from its own source, so a rule written there survives until the next install or refresh and then disappears without a trace.
- Write repository-specific rules **outside** every managed block, or inside a block this skill owns.
- For a globally installed rule, the durable target is the **package source** the block is generated from, not the installed file. Propose the change to that source and let the operator apply it through the package's own release path.
- When you cannot determine which block owns a region, do not guess. Report the rule as drafted and name the ownership question.

Format each rule as an imperative with its reason attached:

```markdown
- <Imperative rule>. <Reason, one clause.>
```

Before adding, search for a rule that already covers the situation. Amend it in place instead of adding a near-duplicate, and show the operator the replacement rather than the addition.

## `emit-agent-memory` — durable agent memory

Target: the host's durable memory mechanism when one exists, resolved from the current agent surface rather than assumed. When the host has none, propose a repository-tracked knowledge file and say that it is a fallback with a different audience.

Authorization: covered by invoking this skill. Memory is private to the operator's environment, so it is the correct home for facts that should not enter the repository.

Each note carries one fact and states:

- the fact, in one or two sentences;
- why it is not derivable from the repository;
- what would make it stale;
- absolute dates, never relative ones.

Do not write a note that duplicates an ADR. The ADR is the durable public record; memory holds what cannot live in the repository.

## `emit-work-log` — run narrative

Default path: alongside the run evidence it describes, such as `context/implementation-runs/<run-id>/closeout.md`. For work with no run directory, follow the repository's existing log or journal convention, and propose one only when none exists.

Authorization: repository-local write covered by invoking this skill.

```markdown
# Closeout: <work> (YYYY-MM-DD)

**Request:** <what was asked>
**Delivered:** <what actually shipped>
**Identifiers:** <branch, commits, PR, run ID>
**Terminal outcome reached:** <selected outcome, and the outcome actually proved>

## Gates

<Exact results, including anything deferred, blocked, or not applicable, copied from the delivery evidence rather than summarized.>

## Open obligations

| Obligation | Owner | Trigger to close |
| --- | --- | --- |

## Discarded during triage

<Candidates examined and rejected, so the filter is auditable.>
```

## External knowledge base

Target: whatever the operator actually uses, named explicitly. There is no default.

Authorization: explicit, per destination, per closeout. Sending content outside the repository publishes it; it may be indexed or cached even if later deleted.

Before sending, verify no secret, credential, customer record, or personal machine path appears in the payload. Present the exact content and destination and wait for approval. If approval is unavailable, keep the draft, emit every repository-local target, and report the external target as `partial` with the exact authorization needed.

## Federation record

Every repository-local emission returns one record for `project-context-initializer`:

| Field | Value |
| --- | --- |
| path | `<repository-relative path>` |
| purpose | <what a reader gets from it> |
| owner | task-closeout |
| producer run | <run ID or request> |
| source revision | <commit the work was based on> |
| content hash | <SHA-256 of the written bytes> |
| consumers | <which skills should read it> |
| contradicts | <existing context claim this invalidates, or none> |

Do not edit an initializer-owned index directly. Hand it the record.
