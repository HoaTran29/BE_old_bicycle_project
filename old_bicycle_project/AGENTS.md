---
trigger: always_on
---

# AGENTS.md

This repository is a **Java 21 / Spring Boot / PostgreSQL backend** for the Old Bicycles Marketplace project.

## Source Of Truth

- Project-local agent system: `.agents/`
- Main spec: `../SRS-Old-Bicycles-Marketplace (1).md`
- Assessment and planning docs: `docs/ai/requirements/` and `docs/ai/planning/`
- Prefer `.agents` over `.claude` when both contain similarly named skills
- Supabase project PostgreSQL ID: kfkzxghznwgbbarfsqre.

## Required Protocol

1. Pick the smallest relevant agent from `.agents/agents/`.
2. Read that agent file.
3. Read only the skills listed in that agent's `skills:` frontmatter.
4. Apply those rules to the task.
5. When using any agent, skill, or workflow, explicitly tell the user which ones are being used and why before doing substantial work. After task is done, announce to the user which ones are being used again.

## Development Lifecycle

- For new features, requirement writing, design refinement, implementation, verification, or knowledge capture, automatically follow the local `.agents` development lifecycle instead of improvising an ad-hoc flow.
- The canonical lifecycle skill for this repository is `.agents/skills/dev-lifecycle/`. Use it by default whenever the task matches end-to-end feature delivery or a named lifecycle phase.
- Prefer the closest matching workflow chain from `.agents/workflows/` and `dev-lifecycle` references. The default sequence is:
  1. `new-requirement` or `review-requirements`
  2. `review-design` when design decisions matter
  3. `plan` or `update-planning`
  4. `execute-plan`, `create`, or `enhance`
  5. `check-implementation`
  6. `writing-test`
  7. `capture-knowledge`
- If the task starts in the middle of the lifecycle, resume from the nearest correct step instead of restarting from the beginning.
- If `dev-lifecycle` is unavailable or broken, say that briefly and use the equivalent local `.agents/workflows/` chain as the fallback.
- If an external lifecycle skill is installed later, use it only when it does not conflict with the project-local `.agents` rules.
## Default Routing

- Progress reports, SRS checks, gap analysis: `requirements-analyst`
- Backend code changes: `backend-specialist`
- Schema or Flyway work: `database-architect`
- Authentication, authorization, ownership: `security-auditor`
- Test work or regression protection: `test-engineer`
- Root-cause investigation: `debugger`
- Multi-domain work: `orchestrator`
- Documentation only when explicitly requested: `documentation-writer`

## Project Rules

- Use PowerShell-friendly commands and prefer `rg` for search.
- Keep recommendations anchored to the actual stack. Do not default to Node.js, React, mobile, game, or SEO workflows unless the user explicitly changes scope.
- Treat a backend feature as `Done` only when API flow, service logic, security, migrations, major SRS rules, and basic verification are aligned.
- If code and SRS disagree, state the mismatch explicitly instead of smoothing it over.
- Do not infer completion from entities or repositories alone.
- All Vietnamese prose written from this point onward must use proper Vietnamese diacritics by default.
- This diacritic rule applies to code comments, JavaDoc, user-facing strings, documentation, planning notes, knowledge notes, and any other Vietnamese explanatory text.
- Do not strip Vietnamese diacritics for convenience. Only keep text ASCII when there is a real technical constraint such as identifiers, slugs, protocol fields, environment variables, file names that must remain ASCII, or compatibility-sensitive tooling.
- Normal code identifiers should remain ASCII unless there is a strong project-specific reason to do otherwise.

## Working Standard

For backend work, the minimum review chain is:

`controller -> service -> repository -> entity -> migration -> security -> test`

If one link in that chain is missing, the feature is at best `Partial`.

## User Visibility

- If an agent is selected from `.agents/agents/`, state its name in a short progress update before acting on it.
- If one or more skills are used, state the skill names and the reason they are relevant.
- If a named workflow is being followed, state that workflow briefly so the user can understand how the task is being executed.
- Do not silently switch to a specialized agent or skill for non-trivial work.

## Knowledge Capture

- After any large task involving code creation, code edits, refactoring, architecture changes, debugging, or any work that introduces useful new programming knowledge, write or update a beginner-friendly knowledge note in `docs/knowledge/`.
- Write knowledge notes in Vietnamese with proper diacritics by default, with wording suitable for a first-year university student who is new to programming.
- Assume the reader may only know basic ideas such as variables, functions, classes, and HTTP at a very early level.
- Explain both the underlying concept and how that concept was applied in the task that was just completed.
- Define important terms clearly before using them in longer explanations. Do not rely on jargon without explanation.
- If an English technical term is necessary, explain it immediately in simple Vietnamese the first time it appears.
- Prefer this explanation order when relevant:
  1. Problem or context
  2. Definition of the concept
  3. Why it matters
  4. Small example
  5. How it was applied in this project
  6. Common mistakes or misunderstandings
- When a concept is abstract, include a short concrete example or suitable code snippet, and add a before/after example when that helps make the difference obvious.
- Separate clearly between `definition`, `example`, and `application in the project` so the reader does not have to infer the structure.
- Use short sections, short paragraphs, and simple sentences. Prefer clarity over compactness.
- Make cause-and-effect explicit. State not only what changed, but why that change prevents a bug, improves safety, or matches the SRS better.
- Before creating a new knowledge note, review `docs/knowledge/` to see whether the topic already exists.
- If a related note already exists, extend it with missing definitions, missing examples, or clearer explanations instead of creating duplicate content.
- If no related note exists, create a new Markdown file under `docs/knowledge/` with a focused title.
