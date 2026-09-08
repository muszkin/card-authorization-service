---
name: implementation-planning
description: Plan non-trivial software changes through repository reconnaissance, iterative requirements clarification, a Socratic challenge of assumptions, explicit happy paths and edge cases, evidence-based technology decisions, and independently implementable vertical slices with user-surface E2E verification. After plan approval, recommend and confirm an implementation profile, multi-agent orchestration mode, terminal outcome and configurable model policy for implementation-orchestrator. Use when the user asks to plan, design, scope, or break down a feature or broad change, or when an ambiguous request needs discovery before implementation.
---

# Implementation Planning

Turn an idea or ambiguous change request into an evidence-backed plan that a fresh executor, preferably a subagent when available, can implement slice by slice and verify immediately.

Use the user's language. Keep planning separate from implementation: inspect read-only, create or update the agreed planning artifact, and stop for approval before changing product code, infrastructure, data, or external systems. Do not create a branch, commit, or deploy unless the user separately asks for it.

## Core invariants

- Inspect before asking. Do not ask the user for facts available in the repository, its documentation, runtime, issue context, or existing tests.
- Track two independent axes: provenance (`Observed`, `User-confirmed`, `Inferred`, `Unknown`, `Contradiction`) and decision state (`N/A`, `Proposed`, `Undecided`, `Decided`, `Accepted risk`, `Out of scope`). Never present an inference or proposal as a fact.
- Ask one material question at a time. Prefer a concise recommendation with real trade-offs when choices are known.
- Use a readiness condition, not a quota of questions.
- Challenge the clarified assumptions before freezing the solution.
- Do not agree reflexively with the user's preferred solution or execution choice. When evidence exposes a safer, smaller or more efficient alternative, challenge the decision with concrete consequences and a recommendation. The operator has the final word inside their authority and repository/safety constraints; record a conscious override and do not relitigate it without new evidence.
- Prefer the existing stack and existing project patterns. Add technology only when a requirement demands it.
- Plan by user-visible vertical slices, never by technical layers.
- Every slice must include its own focused tests and the strongest practical E2E proof through the product's real user surface.
- Produce a threat model whenever the work touches authentication, authorization, secrets, user-supplied input reaching a sink, payments, personal data, file uploads or a public network boundary. Name the trigger explicitly, and promote each material threat either to a slice acceptance test or to an owned accepted risk.
- A self-contained brief does not automatically make a slice safe to run in parallel. Prove both dependency and change-surface independence.
- Make the execution topology explicit: repository-proven static/Sonar/E2E commands, worktree resource isolation, independent review focus, integration checks, CI targets, staging trigger, and production handoff boundary.
- Keep quality coverage separate from gate cadence: every plan defines the complete final static, test, Sonar, independent-review and real-surface E2E obligations even when a later quick profile defers some per-slice gates to final verification.
- Do not mark the plan ready while a material blocker is unresolved or represented by `TBD`, `TODO`, or an implicit guess.

## Eligibility and target gate

Before reconnaissance:

1. Confirm that the request is non-trivial or materially ambiguous. If it is a mechanical edit with explicit behavior, location, and verification, say that the full planning workflow is unnecessary and return to the ordinary task workflow unless the user explicitly wants a formal plan.
2. Resolve the target repository and module from the conversation and current workspace. If exactly one target is evident, use it. If multiple repositories or modules are plausible, ask one target-selection question before scanning and do not inspect unrelated candidates. If no target is available, ask for a repository path or attached source artifacts; for a genuinely greenfield request, explicitly switch to an evidence-limited chat draft. Do not pretend reconnaissance occurred.
3. Adapt to the environment. In a non-Git project, skip Git-specific checks. Without read access, request the missing source or produce only an explicitly evidence-limited draft. Without write access, keep the artifact in chat. Without network access, use locally proven versions and mark any consequential current-version decision as requiring verification rather than guessing.
4. In a non-interactive run, return the material unresolved questions and stop at the relevant gate. Do not manufacture user decisions.

## State machine

Follow this sequence:

`reconnaissance -> clarification -> readiness gate -> Socratic attack -> solution contract -> technology decisions -> vertical slices -> plan audit -> user approval -> execution recommendation -> operator execution decision -> implementation handoff`

If a later phase exposes a material unknown, return to clarification. Do not paper over it downstream.

## 1. Reconnaissance

Announce that planning has started and that implementation will wait for plan approval.

Build a bounded evidence map before asking detailed questions:

1. Look first for the resolved initialized-project router named in active project instructions, falling back to `context/map/INDEX.md`:
   - Read the index and the manifest path declared by it before a broad filesystem scan; the default manifest path is `context/map/manifest.json`.
   - Check scope, source revision, generated time, dirty-state caveat, coverage verdicts, exclusions, contradictions, and unresolved questions against the current repository state.
   - Follow the planning route in the index, including `risks-and-unknowns.md`, preserved related artifacts, and the nearest mapped `.agents/project-context.md` files for the affected paths. For greenfield work, also read applicable `context/greenfield/**` artifacts.
   - Treat generated context as a navigation and evidence map, not authority. Re-open current code, runtime evidence, canonical documentation, and high-risk contracts before relying on consequential claims.
   - If the map is missing, incompatible, partial, or stale for the affected scope, fall back to direct reconnaissance. Refresh or recommend the `project-context-initializer` workflow when a repository-wide remap is warranted; do not block a bounded plan merely because initialized context is absent.
2. Locate the selected repository root and inspect current branch, working-tree status, recent relevant history, and repository instructions when Git is present.
3. Map top-level directories and the areas likely to own the requested behavior, reconciling them with the context coverage ledger when present.
4. Enumerate Markdown and documentation sources while honoring version-control ignores and excluding generated, vendor, dependency, cache, and archive trees unless the task targets them. Read all applicable instruction files, root and affected-module READMEs, documentation indexes, architecture documents, ADRs, product specs, and directly relevant Markdown in full. For a large documentation set, inventory the rest by path and headings, then fully read everything that may change the plan. Stop expanding the inventory once the authoritative documentation map is complete.
5. Inspect manifests, lockfiles, build and test commands, CI configuration, routes or entrypoints, schemas and migrations, integration boundaries, and existing E2E setup.
6. Trace the current user or system flow end to end. Find analogous implementations and the tests that protect them.
7. Determine whether the work is greenfield or brownfield and record the evidence for that conclusion. Do not infer greenfield status from a clean working tree.
8. If the scope has independent investigative lanes and subagents are available, delegate bounded read-only passes such as current flow, data/contracts, UI and E2E, and prior decisions. Give each pass raw context and a distinct question; synthesize their evidence locally.

Do not read every source file indiscriminately. Start broad, then fully read the files that define behavior or constraints. Do not mutate product files during reconnaissance.

Before the first question, give a short checkpoint:

- what exists now;
- which sources establish it;
- what appears reusable;
- which material facts remain unknown.

## 2. Clarification loop

Ask the highest-impact unanswered question, then update the decision ledger. Ask one question per message unless the user explicitly requests a batch.

Prefer multiple-choice options only when they are genuinely exhaustive enough to accelerate a decision. Lead with the recommended option and explain its consequence. Use an open question when options would constrain discovery prematurely.

If the user delegates a choice, make the decision, explain the basis, and record it. Reconfirm an inferred answer only when getting it wrong would materially change scope, behavior, data, security, rollout, or cost. The agent may recommend but must not self-accept any material risk; acceptance requires the user or a named accountable owner.

Do not mirror the user's preferred answer merely to reduce friction. Challenge it when repository evidence, risk, scope or delivery economics disagree. State the strongest concrete counterargument and recommendation once; after an informed operator decision within valid authority, record it and proceed.

Continue until the readiness gate can be answered from evidence or explicit decisions:

- Who is the actor or consumer, and what problem or outcome matters to them?
- What is the current flow and the desired flow?
- What is in scope and explicitly out of scope?
- Which business rules, roles, permissions, and preserved behaviors apply?
- Which data, state transitions, integrations, and compatibility contracts are involved?
- What failure, recovery, concurrency, and abuse cases matter?
- Which product and operational constraints are measurable?
- What proves success, and on which real user surface can it be verified?
- What rollout, migration, rollback, or support constraints apply?

Do not stop because the conversation feels long. Stop when no unresolved answer above can materially change the solution or slice boundaries.

## 3. Readiness gate

State the proposed build in one compact paragraph and list any remaining unknowns by impact.

Pass only when:

- the target behavior can be described without ambiguous verbs such as "support", "handle", or "improve";
- success and non-goals are explicit;
- preserved behavior and compatibility boundaries are known;
- material unknowns are resolved or explicitly accepted as risks;
- the verification surface is identifiable.

If the gate fails, return to the clarification loop.

## 4. Socratic attack

Do not merely polish the favored solution. Try to disprove the assumptions that lead to it.

Create a risk-ranked assumption ledger. Challenge related assumptions using:

`assumption -> strongest counterexample or failure mode -> available evidence -> consequence if false -> decision`

Cover the lenses that are material to this work:

- Is the problem real, and is a code change the smallest effective solution?
- Can an existing flow, component, contract, or operational process solve it?
- Will the intended actor discover and successfully complete the flow?
- Which business rule is underspecified or internally inconsistent?
- What breaks backward compatibility, stored data, integrations, or existing users?
- How can permissions, user input, secrets, payments, or shared resources be abused?
- What happens under retries, duplicates, concurrency, partial failure, offline operation, or timeouts?
- What makes the feature inaccessible, too slow, unobservable, irreversible, or hard to support?
- Can the proposed behavior be tested through the real product surface without hidden setup?
- What is the strongest credible alternative, including a no-build or narrower option?

When useful and available, ask an independent challenger subagent to review the raw request, evidence, and provisional assumptions without revealing the preferred design. Treat its output as hypotheses to verify, not authority.

Bring only material challenges back to the user, one at a time. Group requirements that share the same assumption so the round does not become performative interrogation. Update or reject assumptions explicitly. Passing this phase may confirm the original direction.

Apply the same discipline later to execution choices. A request for `quick-*` does not override a migration, security, public-contract or cross-service risk; a request for maximum ceremony on a tiny isolated change should be challenged when it adds cost without changing the final proof.

## 5. Freeze the solution contract

Define WHAT before HOW:

- goal, actors, and user-visible outcome;
- numbered happy path from the actor's perspective;
- edge cases, error states, recovery behavior, and preserved behavior;
- roles, permissions, data lifecycle, and integration contracts;
- measurable non-functional constraints;
- in-scope and out-of-scope boundaries;
- acceptance criteria and observable evidence;
- rollout, migration, compatibility, observability, and rollback expectations.

Offer two or three approaches only when there is a meaningful decision. Use real alternatives, explain trade-offs, recommend one, and record why the others were rejected. Apply reuse-first and remove speculative scope.

## 6. Decide technologies

For brownfield work, start from versions and conventions proven by manifests, lockfiles, build configuration, and existing code. Deviate only when a requirement cannot be met safely within them.

For a genuinely new technology or upgrade, prefer a dedicated evidence pass. When the choice is consequential or the alternatives are genuinely open, hand the question to [`research-spike`](../research-spike/SKILL.md) and consume its verdict and version evidence rather than settling it inline. Planning still owns the version pin, the slicing and the gate contract; research authorizes nothing.

When the question is small enough to settle inline:

1. Research current official primary documentation and the current stable release at planning time.
2. Check compatibility with the project's runtime, deployment target, licenses, security constraints, and existing ecosystem.
3. Compare one or two credible alternatives when the choice is consequential.
4. Record the exact version or version policy, the requirement that drives it, rationale, rejected alternatives, migration cost, and operational impact.

Never select technology because it is novel or because a static registry says it was once preferred.

## 7. Design vertical slices

Read [references/vertical-slices.md](references/vertical-slices.md) in full before decomposing the work.

Start with the smallest walking skeleton that creates observable value through the real product surface. Each slice must:

- express one capability as "the actor can ...";
- include the minimum UI or interface, application logic, data, configuration, documentation, and observability needed for that capability;
- be implementable by a fresh executor from its own brief, using a subagent when available;
- end in a deployable or mergeable coherent state;
- carry its own focused automated checks and immediate E2E verification;
- own named edge cases and acceptance criteria;
- state prerequisites, contracts consumed and produced, likely change surface, and rollback or fallback;
- state the repository-proven focused/static/Sonar/E2E commands, independent review focus, post-merge integration check, and resources that must be isolated across worktrees;
- fit one implementation and review cycle when it can ship atomically. If safe delivery requires multiple stages, each stage packet must fit its own implementation and review cycle.

Do not create slices named Database, Backend, API, Frontend, Testing, Refactor, or Documentation. Fold those concerns into the first capability that needs them. A shared enabler may stand alone only when it has an independently observable and verifiable outcome; otherwise include it in the first walking skeleton.

Distinguish a capability slice from its delivery stages. The capability must still end in an actor-visible outcome and E2E proof. When safe delivery requires ordered stages such as expand, backfill, cutover, and contract, keep them inside the capability as separate stage packets rather than calling them vertical slices. Each packet gets its own scope, review cycle, tests, operational verification, rollback, entry and exit conditions, and required authorization. A plan never authorizes deployment, migration, backfill, cutover, or contract cleanup.

Build a dependency DAG. Mark slices as parallel-safe only when they have no unresolved contract dependency and no conflicting files, migrations, generated artifacts, shared state, or high-churn integration points. Slices that are independently briefed but share those surfaces must be sequenced.

Give each slice one explicit verification verdict:

- `real-surface E2E` for interactive products;
- `consumer-boundary E2E` when the public API, event, executable, or library consumer is the product surface;
- `strongest non-E2E evidence` only when the real surface is temporarily inaccessible, with the reason, residual risk, accountable exception owner, and a concrete plan to close the gap;
- `blocked` when no trustworthy proof is currently possible.

Use the closest available representation of a real user:

1. Web, desktop, or mobile UI: run the real app and navigate, click, type, or gesture through the rendered interface. Plan local UI E2E in headed mode unless project instructions or the execution environment require otherwise; record the reason for any exception. Direct requests may prepare deterministic data but must not replace the primary user journey.
2. CLI or TUI: launch the real executable and exercise arguments, stdin, keys, stdout, stderr, exit status, and user-visible side effects.
3. API-only service or library: exercise its public consumer boundary end to end and state why no interactive UI exists.

Use real internal boundaries. Mock only external systems that are costly, irreversible, unsafe, or nondeterministic, and document the substitute. Cover the happy path plus the highest-risk edge case for every slice. Prefer stable roles, labels, and accessible names over implementation selectors. Include fixtures, cleanup, start commands, exact verification commands, expected observations, and retained evidence such as screenshots or logs when the project supports them.

If an interactive product has no E2E harness, either include the smallest reusable harness in the first capability that needs it, obtain an explicit owned exception with strongest available evidence, or mark the slice blocked. Never replace a possible UI journey with requests merely because requests are easier. Never invent an existing command: distinguish commands confirmed in the repository from commands the slice will introduce.

## 8. Write the plan artifact

Discover and follow the repository's existing planning convention. If none exists, propose `docs/plans/YYYY-MM-DD-<topic>.md`. If the user wants chat-only planning, keep the same structure in chat.

Read [references/plan-template.md](references/plan-template.md) in full before writing. Produce one auditable artifact rather than a collection of overlapping drafts. Cite the repository paths, symbols, commands, documentation, and external primary sources that shaped decisions.

Set the artifact to `draft` while questions remain and `ready-for-approval` only after the audit passes. Do not stage or commit it unless requested. If its location is ignored by version control, say so explicitly before handoff.

When an initialized project-context router/manifest is present, do not silently edit its generated index. Instead, return a reconciliation record with the plan path, safe content SHA-256, producer, source revision, and current approval status so `project-context-initializer` can federate the new artifact. A greenfield bootstrap requires a durable plan path; when no project convention or downstream path exists, honor the initializer fallback `context/greenfield/implementation-plan.md` rather than leaving the only copy in chat.

## 9. Audit the plan

Review the complete artifact with fresh context. When practical, use an independent reviewer that receives the agreed contract and the plan, not the intended verdict.

Reject or revise the plan if any check fails:

- Every requirement, happy-path step, edge case, and accepted risk maps to a slice and a verification.
- No slice is merely a technical layer or a bag of unrelated changes.
- Every capability slice has one user-visible outcome, a complete brief, focused tests, and an explicit verification verdict. Real-surface E2E is required whenever it is feasible; exceptions are owned and time-bounded.
- Non-visible rollout stages stay inside a capability slice as separately reviewable and separately authorized packets with operational proof, semantic-compatibility checks, and rollback.
- Slice dependencies form a valid DAG, and parallel claims survive a change-surface conflict check.
- Parallel claims also survive a runtime-resource check covering every applicable resource in the orchestrator's [resource isolation contract](../implementation-orchestrator/references/resource-isolation.md).
- Every slice defines the RED acceptance proof, static analysis, Sonar disposition, independent review focus, real-surface E2E, and the feature-head integration check expected by `implementation-orchestrator`.
- The plan defines the complete final feature gate chain independently of the later profile recommendation; any proposed quick cadence identifies what may be `DEFERRED_TO_FINAL` without calling it passed.
- Release topology and required CI are derived from repository evidence; the plan does not assume `main`, `develop`, staging, or a separate production branch.
- No material question, placeholder, contradiction, or unverified assumption remains hidden.
- Technology decisions are requirement-driven and current where freshness matters.
- Compatibility, migration, observability, rollout, and rollback are covered in proportion to risk.
- The threat-model triggers were checked explicitly, and every material threat maps to a slice test or an owned accepted risk.
- Every added or upgraded dependency has a recorded resolved version, advisory status and license, and no plan step introduces a secret value into a tracked file.
- The plan follows project instructions and existing standards.
- An executor can identify what not to change as clearly as what to change.

Fix supported issues inline, then present:

- the agreed solution in a short summary;
- the plan artifact path or the full chat plan;
- the reconciliation record above when initialized context exists;
- slice order and safe concurrency groups;
- the execution-topology and gate handoff for `implementation-orchestrator`;
- the evidence-based execution-profile recommendation and quick-profile eligibility verdict;
- remaining accepted risks;
- the exact approval needed to begin implementation.

Stop and wait for plan approval. Planning completion is not implementation authorization.

## 10. Recommend and confirm execution after plan approval

Only after the operator approves the plan, read the current `implementation-orchestrator` [execution profile contract](../implementation-orchestrator/references/execution-profiles.md) and make one evidence-based recommendation:

- `quick-dev` for a small, focused non-bug change with healthy baseline, narrow ownership, no protected/high-risk surface and a simple rollback;
- `quick-bug-fix` for a similarly small bug only when reproduction before editing is practical and mandatory;
- `standard` as the fallback for ordinary feature/change delivery with full per-slice gates;
- `bug` for a defect requiring reproduction plus full per-slice gates.

Recommend `fire-and-forget` by default. It remains bounded multi-agent work with isolated worktrees and durable evidence; it does not mean uncontrolled parallelism or expanded authority. Recommend `supervised` when the operator needs explicit checkpoints because decisions, external side effects or learning goals justify them.

Define a terminal outcome rather than the vague instruction "finish": `local-green`, `ready-pr`, `integration-merged`, `staging-green`, `production-pr-ready` or `production-green`. For `production-green`, list the extra authority, artifact provenance, safe production smoke/E2E, rollback rule, health signals and observation window that implementation must confirm.

Recommend the configurable `daily-coding` model policy defined by the orchestrator's [execution profile contract](../implementation-orchestrator/references/execution-profiles.md), which resolves the current general-purpose coding tier from the host rather than naming fixed models. Name it as a default, not a requirement, and preserve any explicit operator override. Ask whether automatic escalation from `quick-dev -> standard` and `quick-bug-fix -> bug` (or `standard` when the issue is not a bug) is allowed when risk/scale evidence invalidates quick eligibility.

Ask one compact decision immediately before implementation begins, covering:

1. profile;
2. `fire-and-forget` or `supervised`;
3. terminal outcome;
4. default or overridden worker/reviewer models;
5. automatic profile escalation.

If the operator chooses a materially mismatched option, challenge it once with repository/plan evidence and the cost or risk consequence. The operator's informed final decision wins within authorization and safety controls. Record the selected contract in the plan/handoff. That current trusted selection may be consumed by `implementation-orchestrator`; it should restate the contract before its first side effect rather than ask a duplicate question.

Implementation authorization covers only the approved plan and recorded execution envelope. The planning skill still does not edit product code, create implementation worktrees, push, merge or deploy.
