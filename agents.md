# Agent Coding Guidelines

These guidelines are mandatory for any agent making code changes. Read this file before coding.

## Startup Rules

- Before making changes, read this file and any project summary or README files that describe current architecture and behavior.
- Treat those files as required context for implementation and review work.

## User Intent Rules

- Do not make code changes until the user explicitly asks for implementation work.
- If the user asks for planning, review, investigation, or explanation, do not edit files.
- If the user's intent is ambiguous, inspect the code and propose a plan instead of changing files.

## General Rules

- Do not rewrite unrelated code.
- Do not remove existing behavior unless the user explicitly asks for it.
- Every code change must include an added or updated test that covers the change.
- Documentation-only changes do not need tests.
- If adding a test is genuinely impossible, explain why before finishing.
- If a check cannot be run, clearly say why.

## Architecture Rules

- Keep canonical business rules in one clear owner. Do not split them across frontend and backend unless the user explicitly asks for that tradeoff.
- Keep browser-local state limited to UI concerns such as selection, loading, and error presentation.
- Before adding persistence, networking, or new state ownership patterns, explain the architecture impact.

## Code Quality

- Keep validation in the layer that owns the data or behavior.
- Avoid duplicating business rules across layers unless the duplicate is only for display or user guidance.
- Keep state transitions clear and testable.
- Avoid broad refactors while implementing narrow feature requests.

## Frontend Rules

- Keep UI state synchronized with backend responses when a backend exists.
- Keep layouts stable and responsive.
- Make sure text, controls, and panels do not overlap.
- Keep controls discoverable and close to the workflow they affect.
- Run the project’s frontend build or test command after frontend changes.

## Backend Rules

- Keep the backend as the source of truth for business rules.
- Validate inputs server-side.
- Add or update backend tests for every backend code change.
- Keep request and response objects stable unless changing the API is part of the task.
- Avoid leaking internal state that users should not see.
- Run the project’s backend test command after backend changes.

## Testing Rules

- When fixing a bug, add or update a test that reproduces the failure before or alongside the fix.
- When adding a browser route, add a test or check proving the route can load directly by URL.
- When refactoring, keep tests focused on behavior rather than implementation details.
- Do not claim behavior is unchanged unless relevant tests pass or you clearly state verification was not completed.

## Design Rules

- Avoid decoration that interferes with readability or interaction.
- Make responsive behavior deliberate, not accidental.

## Git Rules

- Before making a commit, make sure all relevant tests pass.
- When the user says `commit and push`, create the commit first and push only after the commit succeeds.
- Use a short, meaningful commit title.
- Include a commit body when the reason for the change is not obvious from the title.
- Update relevant README or project summary documentation before committing changes that alter setup, architecture, user workflows, or important behavior.
- Keep `.gitignore` updated when new generated files appear.
- Inspect unexpected untracked files before deciding whether they belong in a commit.
- Never revert user changes unless the user explicitly asks.

## Review Rules

- In review mode, prioritize behavior regressions, rule mismatches, missing or stale tests, and architecture drift.
- Call out maintainability issues when they make future behavior changes riskier.

## Change Safety Rules

- Ask before making changes that introduce persistence or external networking.
- Ask before materially redesigning the UI.
- Ask before changing business rules, game rules, or other core domain behavior.
- Ask before altering build, test, or run workflows in a way that affects how contributors use the app.

## Local Run Rules

- If a server is already running in an attached terminal session, try stopping it with `Ctrl-C` before using process-killing commands.
- If the default port is busy, report the conflict instead of silently switching ports unless the user asks for a fallback.
