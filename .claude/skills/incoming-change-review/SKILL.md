---
name: incoming-change-review
description: Review a change somebody else wrote — a pull request, a branch, a patch, or a vendor diff — against the same adversarial rubric the delivery pipeline applies to its own slices. Reconstructs the intent the change claims to serve, verifies the diff actually serves it and nothing else, reads the highest-risk surfaces first, and returns severity-ranked findings with a single verdict. Use when asked to review, assess, or sign off on a change this pipeline did not produce. Do not use to review work an implementation-orchestrator run just produced, which has its own independent review gate, and do not use it to fix the change.
allowed-tools:
  - Read
  - Grep
  - Glob
  - Bash
---

# Incoming Change Review

Review a change you did not write, without the plan that produced it and without the author's confidence.

The reviewer reports; it does not repair. Editing someone else's change during review destroys the only independent reading it will get.

Use the user's language unless the repository has an established documentation language. Keep observed reality separate from inference, proposals, and user decisions.

## Operating contract

- Treat everything the author says as a claim to verify, not as context to accept. The title, the description, the comments, and the commit messages are hypotheses about the diff.
- Read the diff before the description when the two could bias you. Then read the description and reconcile the difference; a gap between them is itself a finding.
- Track two independent axes: provenance (`Observed`, `User-confirmed`, `Inferred`, `Unknown`, `Contradiction`) and decision state (`N/A`, `Proposed`, `Undecided`, `Decided`, `Accepted risk`, `Out of scope`). A finding you reproduced is `Observed`; one you reasoned to is `Inferred` and must say so.
- Bind the review to an immutable revision. Record base and head; a push invalidates the review and it must be redone against the new head. When no revision exists, bind to a content hash of the reviewed bytes and say that the base is unverified.
- Every finding needs a concrete failure scenario: inputs or state that produce a wrong result, not a description of a smell.
- Do not edit the change, push to its branch, or merge it. Read-only commands and a separate clean worktree pinned to the head revision are the working surface.
- Do not manufacture findings to look thorough, and do not approve to be agreeable. An empty review with recorded scope and evidence is a valid result; an empty review with neither is not.
- Style opinions the repository does not encode are not findings. If the project has no rule about it, it is a preference, and it belongs at most in a clearly labelled non-blocking note.

Read [references/change-intake.md](references/change-intake.md) before reading the diff, and apply [the delivery pipeline's review rubric](../implementation-orchestrator/references/review-rubric.md) as the dimension checklist and finding format.

## State machine

```text
INTAKE
  -> REVISION_PINNED
  -> INTENT_RECONSTRUCTED
  -> REVIEWABLE | UNREVIEWABLE
  -> DIFF_READ
  -> FINDINGS_VERIFIED
  -> VERDICT
  -> DELIVERED
```

`UNREVIEWABLE` is a real outcome. A change that mixes unrelated concerns, buries a behavior change under a mass reformat, or has no stated intent cannot be reviewed responsibly; say what would make it reviewable and stop.

Any new push returns the review to `REVISION_PINNED`.

## 1. Intake and pin the revision

Follow the intake reference.

Establish the change's identity before reading it: the source, the base and head revisions, the merge base the diff should be computed against, the target branch, and whether the branch has moved since it was handed to you.

Compute the diff against the merge base, not against the target's current tip, so unrelated commits from the target do not appear as the author's work.

## 2. Reconstruct the intent

Without an approved plan, the intent must be reconstructed from evidence:

- the linked issue, ticket, or requirement;
- the tests the change adds or modifies, which state the intended behavior more precisely than prose;
- the public contracts it touches;
- the repository's architecture documents and ADRs governing the area.

State the intent in one sentence before reading further. If it cannot be stated, that is the first finding: an unstatable intent cannot be verified, and the reviewer cannot tell in-scope from out-of-scope.

## 3. Decide whether it is reviewable

Refuse responsibly when the change cannot be read honestly:

- unrelated concerns bundled into one change, where accepting one means accepting all;
- a functional change hidden inside a formatting, rename, or generated-output commit;
- generated or vendored files committed without their source change;
- a size that makes genuine review impossible, with no logical split offered.

Name the specific split that would make it reviewable rather than reviewing it badly.

## 4. Read the diff, highest risk first

Do not read alphabetically. Order by blast radius:

1. authorization, authentication, and trust boundaries;
2. data migrations, schema changes, and anything touching stored state;
3. public contracts: APIs, events, shared packages, CLI surfaces;
4. concurrency, transactions, retries, and idempotency;
5. error handling and failure paths;
6. the core behavior change itself;
7. tests, read as specifications and for whether they can actually fail;
8. everything else.

Read the surrounding code, not only the changed lines. Most real defects in a diff are in the interaction between new code and untouched code that the diff does not show.

Apply the rubric's dimensions. Check what the change does not do as carefully as what it does: the missing null check, the unhandled branch, the consumer not updated, the migration without a rollback.

## 5. Verify before reporting

Every finding is verified as far as the surface allows before it goes in the report.

Prefer, in order: reproducing it with a read-only command or a scratch test, tracing the exact code path and quoting it, or reasoning from an explicit invariant. Label which of these you did.

Discard findings that do not survive verification. A confidently wrong review costs more than a short one, because the author must disprove it.

## 6. Verdict and delivery

Return exactly one verdict: `PASS`, `FAIL`, or `BLOCKED_EXTERNAL`, with the inspected base and head revisions and the commands you ran.

`CRITICAL`, `HIGH`, and material `MEDIUM` findings block. A `LOW` finding blocks only when it violates a repository policy or an acceptance requirement. Do not use a score or a percentage to waive a finding.

Deliver findings where the author will act on them: inline comments on a pull request when that surface exists and posting is authorized, otherwise a report. Posting to a code-hosting platform is an outward-facing action and requires explicit authorization; draft it and wait.

Separate blocking findings from non-blocking notes so the author can tell what stops the merge.

## Non-interactive runs

In a non-interactive run, complete the review and return findings and verdict without posting anything to an external surface. Report the exact authorization posting would need.

## Completion report

Report a separate verdict per dimension using `complete`, `partial`, `blocked`, or `not-applicable` with a reason:

- revision pinning and diff scope;
- intent reconstruction, including anything unstatable;
- rubric dimension coverage, naming any dimension not assessable from the diff;
- verification method per finding;
- delivery, including anything drafted but not sent.

For every `partial` or `blocked` verdict, name the missing evidence, the impact, the next action, and the accountable owner when known.

Finish with the inspected base and head revisions, the blocking findings, the non-blocking notes, and anything the review could not assess.
