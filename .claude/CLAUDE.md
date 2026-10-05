<!-- methodology-template: clean-ddd-core-only -->

# Project: lock-clean

Clean DDD tutorial / reference project. A small Spring Boot + Spring Data JDBC
codebase that showcases **optimistic locking across several aggregates** (`Course`,
`Student`, `Subscription`) when a use case runs under multi-thread contention. It
accompanies a public Medium article on optimistic concurrency and inter-aggregate
invariants in Clean DDD (link in `README.md`).

> **Public repository — `.claude/` is versioned on purpose.** This repo lives on
> public GitHub (`github.com`). The `.claude/` memory files document the Clean DDD
> design reasoning, which is itself part of what this project shares with the
> community, so they are committed. Keep all committed content **institution-neutral
> and free of machine-specific setup** — no absolute paths, usernames, or internal
> hostnames. Machine-local Claude settings stay out of git via `.gitignore`
> (`.claude/settings.local.json`). The `@~/.claude/methodology/*` references below
> point at the author's *local* methodology library; they will **not** resolve in a
> fresh clone and are kept as provenance, not as a runnable dependency.

## Methodology in scope

- @~/.claude/methodology/clean-ddd-core.md
- @~/.claude/methodology/persistence-and-transactions.md
- @~/.claude/methodology/testing.md
- @~/.claude/methodology/session-wrap-up.md

## Stack

- Java 21, Maven, Spring Boot 3.4.x parent
- PostgreSQL via the project `docker-compose.yaml`
- Flyway for schema migrations — **no ORM** (Spring Data JDBC + MapStruct per the
  persistence module)
- Lombok for domain/DB-entity shape; MapStruct for DB-entity ↔ domain mapping
- No UI and no application entry point: the use cases are driven from an
  integration test (see `memory/project-context.md`)

## Working rules for this project

- **Discuss before implementing.** Do not implement features without explicit
  consent and only after the design has been examined together. Default to
  architectural discussion; treat "NO CODE" prompts as pure design conversation.
- **Issues are plain.** Create issues with plain `gh issue create` — no assignee,
  no sprint, no external issue-tracker attribution. This is a personal
  demonstration project.
- **Leak-scan `.claude/` before publishing.** `.claude/` is versioned on a public
  repo (only `settings.local.json` is ignored). Before any commit or push that
  touches `.claude/`, scan the changed content for machine/institution leakage —
  absolute paths, the local username, internal hostnames, internal tooling names,
  JDK install paths — and confirm `settings.local.json` is still the lone ignored
  file. Recipe + exact command in `memory/project-context-extended.md`.
- Sessions are spaced out — re-read the memory files at the start of each one to
  reload where we left off.

## Project-specific context

See `.claude/memory/project-context.md` and `.claude/memory/project-context-extended.md`
in this repository. Deep-dive narrative documents (domain context, onboarding notes,
architectural decisions) also live under `.claude/memory/`.

## On-demand resources

- `~/.claude/methodology/INDEX.md` — map of all methodology modules.
- `~/.claude/methodology-log.md` — cross-project learning journal (maintainer-curated; read-only here).
