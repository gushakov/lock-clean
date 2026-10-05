# lock-clean — project context

> **Charter — current facts, scannable index.**
> This file owns the **quick-reference** view of the project as it exists *today*:
> stack, top-level package layout, entry points, run/test commands, repo workflow,
> and any short fact tables (glossary, use-case index, profile table, key files)
> that earn their place by being scannable.
>
> - Update when a stack version changes, a new top-level package appears, a use
>   case is added or renamed, or a run/test command changes.
> - Do **not** put rationale, history, or architectural discussion here. Those
>   belong in `design-notes.md`.
> - Do **not** put recipes or long-form conventions here. Those belong in
>   `project-context-extended.md`.
> - Do **not** keep a per-issue changelog or "Recent Changes" log here. Change
>   history is git's job; memory holds the distilled current state. See
>   `session-wrap-up.md` §"Memory is not a changelog".

## What this is

Demonstration project for **optimistic locking across several aggregates in a Clean
DDD application** under multi-thread contention — the state-based counterpart to the
"Dynamic Consistency Boundary" (DCB) idea for event-sourced aggregates. Companion code
to a public Medium article (linked from `README.md`; rationale distilled in
`design-notes.md`). Public repo on `github.com` (`gushakov/lock-clean`). Tutorial /
reference — not a production system.

## Stack

| Concern | Choice |
|---|---|
| Runtime | Java 21, Spring Boot 3.4.3 parent (`spring-boot-starter-parent`) |
| Build | Maven |
| Database | PostgreSQL 16.3 via project `docker-compose.yaml` (service `db`, database `lockdb`, port 5432) |
| Migrations | Flyway (`flyway-database-postgresql`), scripts in `src/main/resources/db/migration/` |
| Persistence access | Spring Data JDBC (`spring-boot-starter-data-jdbc`), **no ORM** |
| Mapping | MapStruct 1.6.3 (`defaultComponentModel=spring` via compiler arg) + `lombok-mapstruct-binding` |
| Domain shape | Lombok (`@Builder` on validating constructors, `@Value` ID VOs, `@FieldDefaults`) |
| Id generation | JNanoID 2.0.0 behind `IdsOperationsOutputPort` (`crs_`/`stu_`/`sub_` + 6 alphanumerics) |
| Tests | JUnit 5, Mockito + AssertJ (via `spring-boot-starter-test`) |
| UI | none |

## Package layout (Clean DDD)

Base package `com.github.lockclean`.

- `core/` — framework-free.
  - `GenericLockCleanError` — root unchecked error; every port error extends it.
  - `model/` — `Validator` + `InvalidDomainObjectError` (always-valid construction gate);
    aggregates `course/` (`Course`, `CourseId`), `student/` (`Student`, `StudentId`),
    `subscription/` (`Subscription`, `SubscriptionId`).
  - `port/` — `ErrorHandlingPresenterOutputPort` (base presenter, default `presentError`);
    `db/` (`PersistenceOperationsOutputPort`, `PersistenceOperationError`);
    `concurrency/` (`OptimisticLockingError` — neutral ground between the persistence
    *raiser* and the transaction *reactor*); `id/` (`IdsOperationsOutputPort`);
    `transaction/` (`TransactionOperationsOutputPort`, `TransactionOperationsError`).
  - `usecase/{summarygoal}/` — one package per use case holding `*InputPort`,
    `*PresenterOutputPort`, `*UseCase`: `registercourse/`, `registerstudent/`,
    `subscribestudent/`.
- `infrastructure/` — adapters and Spring wiring.
  - `LockCleanApplication` — `@SpringBootApplication` at the package root (scan never reaches `core`).
  - `adapter/db/` — `PersistenceGateway` (single adapter for the persistence port, **no
    `@Transactional`**) + per-aggregate `{course,student,subscription}/*DbEntity` and
    `*DbEntityRepository` (`CrudRepository`), `map/` (`DbEntityMapper` + `MapStructDbEntityMapper`).
  - `adapter/id/JNanoIdGenerator`, `adapter/transaction/SpringTransactionAdapter`.
  - `config/TransactionConfig` — the two `TransactionTemplate` beans (read-write `@Primary`,
    `@Qualifier("read-only")`).
- `src/test/java/` — `core/model/course/CourseTest`; `core/usecase/InlineTransactionOperations`
  (shared tx-port double) + `registercourse/RegisterCourseUseCaseTest` +
  `subscribestudent/SubscribeStudentUseCaseTest`; `infrastructure/adapter/transaction/SpringTransactionAdapterTest`;
  `infrastructure/SubscribeStudentIT` + three `*LoggingPresenter`s.

## Use cases

| Package | Input port method | Writes | Lock-loss reaction |
|---|---|---|---|
| `registercourse` | `registerCourse(title, capacity)` | new `Course` | propagating default (single writer) |
| `registerstudent` | `registerStudent(fullName)` | new `Student` | propagating default (single writer) |
| `subscribestudent` | `subscribeStudentToCourse(studentId, courseId, optional)` | re-saves `Course` + `Student`, new `Subscription` | `doInTransaction(action, onLockDetected)` → warning |

## Entry points

- `LockCleanApplication` is the `@SpringBootConfiguration`; running it starts the context
  (adapters, repositories, Flyway) but there is **no driving adapter** — the application
  does nothing on its own.
- Use cases are plain objects assembled where they are used: the IT constructs them with
  the context's adapters and its own recording presenters. There is no composition root
  bean config in either scope.
- Datasource: `application.yaml` points at `localhost:5432/lockdb`, `postgres`/`postgres`.

## How to run and test

Requires **JDK 21 on `JAVA_HOME`** and Maven.

```bash
mvn test
```

Runs the unit tests (25): construction gate, the two use case interaction tests, and
`SpringTransactionAdapterTest`. `SubscribeStudentIT` is **not** picked up by `mvn test`
(`*IT` suffix, no Failsafe configured). To run it against the compose Postgres:

```bash
docker compose up -d
mvn test -Dtest=SubscribeStudentIT
```

The IT is self-contained (registers its own course and student, fresh ids each run) and
leaves its rows in the database on purpose. Flyway migrates the schema on first bootstrap.

## Repo workflow

- Public GitHub, remote `origin`, default branch `main`; integration branch `dev`
  (at onboarding `dev` existed **only locally** — push it before any PR targets it).
- `.claude/` is versioned (machine-local `settings.local.json` excluded); keep
  committed content institution-neutral and free of machine setup — leak-scan recipe
  in `project-context-extended.md`.
- Standard flow otherwise: `claude/<issue>_*` branch from `dev`, PR `--base dev`.
  Issues are plain `gh issue create` (no assignee, no sprint).
