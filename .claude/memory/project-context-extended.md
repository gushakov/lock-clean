# lock-clean — extended context

> **Charter — deep reference for current facts, recipes, conventions.**
> This file owns the **on-demand deep reference** for what exists today: domain
> model overview, use-case inventory, infrastructure adapters, persistence /
> security / frontend / test specifics, code conventions (Lombok shape, mapper
> wiring, naming patterns), end-to-end recipes, and recorded methodology
> deviations.
>
> - Update when a convention changes, a pattern is added, a new adapter family
>   appears, or a recipe needs revising.
> - Facts only. Rationale ("why we chose X over Y") belongs in `design-notes.md`.
> - Quick-reference one-liners belong in `project-context.md`, not here.

## Domain model

Three small aggregates referencing each other by ID; no aggregate holds another.

- **`Course`** — `CourseId id`, `String title`, `Integer capacity`, `Integer version`.
  Invariants enforced in the constructor: id not null, title not null/blank, capacity
  strictly positive. Equality by `id` only.
- **`Student`** — `StudentId id`, `String fullName`, `Integer version`. Id not null,
  full name not null/blank. Equality by `id`.
- **`Subscription`** — `SubscriptionId id`, `StudentId studentId`, `CourseId courseId`,
  `Boolean optional` (defaults to `false` when null), `Integer version`. The three ids
  not null. Equality by `id`.
- **ID VOs** (`CourseId`, `StudentId`, `SubscriptionId`) — `@Value` wrappers over a
  non-blank `String`; the raw getter is suppressed (`@Getter(AccessLevel.NONE)`) and the
  text form is exposed as `asString()`.
- `version` is a plain nullable `Integer` carried by every aggregate — the optimistic-lock
  token, round-tripped unchanged through the mapper. It is `null` on a freshly built
  aggregate (insert) and set on a reconstituted one (update).

The business rules of the subscription scenario (from the article) live in
`SubscribeStudentUseCase`, not in an aggregate — they are inter-aggregate:

1. At most one subscription of a given student to a given course (no duplicates).
2. A course has a capacity, and **the capacity can change at any time**.
3. A course accepts no further subscription once its subscription count is at or over
   its current capacity.
4. A student holds at most `STUDENT_SUBSCRIPTIONS_LIMIT` (10, a constant on
   `SubscribeStudentInputPort`) subscriptions.

The initiating actor of `subscribestudent` is a **batch system**, not an authenticated
student — which is why `studentId` is an input argument rather than derived from a
principal.

## Use case inventory

- `registercourse` — system registers a new course (title, capacity); single write
  inside `doInTransaction`, success presented via `doAfterCommit`. Single-writer: a lock
  error reaching this write is a wiring surprise and rides to `presentError` as itself
  (pinned by `RegisterCourseUseCaseTest`).
- `registerstudent` — same shape for a new student.
- `subscribestudent` — subscribes a student to a course. Order: construct both ID VOs;
  `subscriptionExistsAlready` → warn-and-return; `obtainCourseById` +
  `countNumberOfSubscribersToCourse` → capacity check → warn-and-return;
  `countNumberOfCoursesSubscribedForByStudent` + `obtainStudentById` → limit check →
  warn-and-return; build `Subscription`; then **one read-write transaction** through
  `doInTransaction(action, onLockDetected)`: the action re-saves the unchanged `Course`
  and `Student`, saves the new `Subscription`, and registers
  `doAfterCommit(presentSuccessful…)`; the handler presents
  `presentWarningIfConcurrentModificationWasDetected(course, student)`. That call is the
  interaction's terminal act.

**The inter-aggregate lock technique** (the project's point): `Course` and `Student` are
saved *unchanged* inside the subscription transaction purely so their `@Version` is
checked and bumped. A concurrent subscription that touched the same course or student
then fails one of those saves with `OptimisticLockingError`; the lambda short-circuits at
the losing save, the transaction adapter rolls back and runs the handler.

Every use case has the same checkpoint shape: a single outermost
`catch (Exception e) → presenter.presentError(e)`; anticipated outcomes present directly
and `return` before the transaction.

## Infrastructure adapters

| Port | Adapter | Notes |
|---|---|---|
| `PersistenceOperationsOutputPort` | `PersistenceGateway` (`@Service`) | three `CrudRepository`s + `DbEntityMapper`; **no `@Transactional`** — Spring Data's `SimpleJdbcRepository` transactions are the per-call floor, demarcation is the use case's |
| `IdsOperationsOutputPort` | `JNanoIdGenerator` (`@Service`) | `SecureRandom`, alphanumeric alphabet, length 6, typed prefixes |
| `TransactionOperationsOutputPort` | `SpringTransactionAdapter` (`@Service`) | two injected `TransactionTemplate`s; `lombok.config` copies `@Qualifier` onto the generated constructor |

Wiring: adapters are discovered by component scan from `LockCleanApplication`
(`infrastructure` root). Use cases are **not** beans: the IT constructs them with the
autowired adapters and its own presenters.

### Persistence gateway specifics

- Reads: the repository call + mapping sit inside `try { … } catch (DataAccessException |
  InvalidDomainObjectError e)` → `PersistenceOperationError` (a corrupt stored row failing
  reconstitution is this port's integrity fault); the not-found `orElseThrow` is **outside**
  the `try`, so it is never re-wrapped.
- Writes: `catch (OptimisticLockingFailureException) → OptimisticLockingError` placed
  **ahead of** `catch (DataAccessException) → PersistenceOperationError`. Catches are
  narrow (`DataAccessException`, never `Exception`) so a programming bug rides raw to the
  use case's checkpoint.
- Insert-vs-update is decided by Spring Data JDBC from `@Version` (`null`/`0` inserts).
- Derived queries on `SubscriptionDbEntityRepository`: `existsByStudentIdAndCourseId`,
  `countByCourseId`, `countByStudentId` — the only read-model-style queries; no custom SQL.

### Transaction adapter specifics

- Port shape (lean): `doInTransaction(Runnable)`, `doInTransaction(boolean readOnly, Runnable)`,
  `doInTransaction(Runnable action, Runnable onLockDetected)`,
  `doInTransactionWithResult(…, Supplier<T>)`, `doAfterCommit(Runnable)`. No `rollback()`,
  no after-rollback hook, no `*WithResult` after-hooks.
- `doInTransaction` / `doInTransactionWithResult` catch **only** Spring's
  `TransactionException` and wrap it into `TransactionOperationsError`; anything the action
  throws passes through untouched (and triggers the rollback).
- The handler overload calls the plain overload and catches `OptimisticLockingError`
  *outside* `execute()` — i.e. after the rollback — then runs the handler. A `null` handler
  degrades to the plain overload.
- `doAfterCommit` registers `TransactionSynchronization.afterCommit()` (fail-loud); with no
  active transaction the action runs immediately.
- Error types: `PersistenceOperationError`, `TransactionOperationsError`, `OptimisticLockingError`
  are **siblings** under `GenericLockCleanError`.

## Persistence specifics

- Schema: `src/main/resources/db/migration/V1.0__Initial_setup.sql` — tables `course`
  (`id`, `title`, `capacity`, `version`), `student` (`id`, `full_name`, `version`),
  `subscription` (`id`, `student_id` FK, `course_id` FK, `optional` default false,
  `version`). All ids `varchar` PKs; `version` nullable `int`.
- `spring.flyway.schemas: public`; `logging.level.sql: debug` so every SQL statement
  (including the `UPDATE … WHERE version = ?` lock checks) is visible in test output.
- No unique constraint on `(student_id, course_id)` — duplicate prevention is the
  `subscriptionExistsAlready` check in the use case plus the aggregate re-saves.

## Test specifics

- `CourseTest` — parameterized unit tests of the construction gate.
- `InlineTransactionOperations` (`core/usecase/`, test scope) — tx-port double for use case
  tests: runs actions inline, after-commit immediately, routes `OptimisticLockingError` to
  the handler (or rethrows on `null`).
- `SubscribeStudentUseCaseTest` — mocked ports; pins: success presented once with the
  unchanged course/student re-saved; lost race on course save or on student save → one
  warning, no success, no `presentError`, later saves skipped; a `PersistenceOperationError`
  inside the transaction → `presentError` as itself; capacity reached → no writes.
- `RegisterCourseUseCaseTest` — success once; the single-writer pin (`OptimisticLockingError`
  → `presentError` as itself); invalid capacity → `presentError`, no write.
- `SpringTransactionAdapterTest` — real `TransactionTemplate` over a mocked
  `PlatformTransactionManager` for error translation and the handler; nested
  `AfterCommitFailsLoudly` over a private `NoOpTransactionManager`
  (`AbstractPlatformTransactionManager` with no-op resources) for the `afterCommit()` contract.
- `SubscribeStudentIT` — `@SpringBootTest(webEnvironment = NONE)`, **no test-managed
  transaction**, real compose Postgres. Registers its own course/student through the use
  cases, captures them with recording presenters that extend the logging ones. Case 1:
  success once, subscription row present, course and student versions each `+1`. Case 2:
  a `RivalWriteBeforeLastRead` decorator around the real persistence port re-saves the
  course row (bumping its version) right before `obtainStudentById`; asserts one warning,
  no success, no `presentError`, no subscription row, course at the rival's version,
  student version unchanged. Rows are left in the DB (fresh ids each run).
- Still absent: ArchUnit rules.

## Code conventions (as established)

- Aggregates: `@Getter`, `@FieldDefaults(makeFinal = true, level = PRIVATE)`,
  `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with `@Include` on the id,
  `@Builder` on the validating constructor (never on the class). Validation through the
  static `Validator` helper (`notNull`, `notNullOrBlank`, `strictlyPositive`) throwing
  `InvalidDomainObjectError`.
- Use cases: `@RequiredArgsConstructor` + `@FieldDefaults(makeFinal = true, level = PRIVATE)`,
  fields `presenter`, `txOps`, `persistenceOps`, `idsOps`, in that order.
- Presenter ports extend `ErrorHandlingPresenterOutputPort`; method names
  `presentSuccessfulResultOf…`, `presentWarningIf…` (incl. the concurrent-modification
  outcome — a warning, not an error).
- Persistence port methods: `obtain*ById` (throws when absent), `count*`, `*ExistsAlready`,
  `save*`.
- DB entities: `@Getter @Setter @Builder`, `@Table`, `@Id`, explicit `@Column("snake_case")`,
  `@Version Integer version`. MapStruct mapper is an **abstract class** implementing the
  hand-written `DbEntityMapper` interface, which carries the `default` ID↔`String`
  converters.
- "POINT OF INTEREST" block comments mark the didactic spots of the article.
- Lombok is preferred over records throughout.

## Non-obvious conventions / recorded deviations (the *what*)

Observed against `persistence-and-transactions.md`; reasoning is in
`design-notes.md` §Methodology assessment.

1. Naming drift from the reference projects: `port/db` (module: `port/persistence`),
   `PersistenceGateway` (module: `*Adapter`), `obtain*` (module: `find*` returning
   `Optional`), `PersistenceOperationError` (module: `PersistenceOperationsError`).
2. No ArchUnit guard pinning `core ↛ infrastructure` or the neutral `port/concurrency`
   placement.
3. No composition root in either scope; use cases are assembled by the test that drives them.

What **conforms**: lean transaction port with the lock-detect overload; `afterCommit()`
fail-loud hook; narrow `TransactionException` translation; `OptimisticLockingError` on
neutral ground as a sibling of the port errors; reads and rule checks outside the
transaction, writes inside one narrow `doInTransaction`, success deferred to after-commit,
a single outermost `catch → presentError`; no `@Transactional` on the gateway; Spring Data
JDBC + MapStruct + Flyway with no ORM.

## Recipe — leak-scan `.claude/` before publishing

`.claude/` is versioned on a public repo; only `settings.local.json` is git-ignored
(it holds machine-local absolute paths, the username and the JDK location). Before
any commit/push that touches `.claude/`, confirm the publishable content is clean:

```
# The path/tooling fragments are literal and generic; replace each <placeholder>
# with your own machine/institution string. Do NOT write those real strings into
# this file — describe them as categories, or the scan would flag (and republish)
# its own recipe.
grep -rniE 'C:/Users|/c/Users|\.jdks|<local-username>|<institution>|<internal-host>|<internal-tooling>' \
  .claude --exclude=settings.local.json     # expect: no matches
git check-ignore .claude/settings.local.json  # must still print the path
```

- Clean scan **and** `settings.local.json` still ignored ⇒ safe to commit.
- The public repo handle (`<owner>/lock-clean`) is fine — it is the repo's own
  identity, not a leak.

## Recipe — run the subscription experiment end to end

```
docker compose up -d
mvn test -Dtest=SubscribeStudentIT
```

Watch the SQL debug log: the success case shows three `UPDATE … WHERE … "version" = ?`
/ `INSERT` statements then the success line; the contention case shows the rival's
`UPDATE "course"` first, then the use case's `UPDATE "course"` affecting zero rows, the
rollback, and the single warning line. To reproduce the article's manual experiment
instead, add a pause before `txOps.doInTransaction` in `SubscribeStudentUseCase` and bump
a `version` by hand while it sleeps. (Requires JDK 21 on `JAVA_HOME`.)
