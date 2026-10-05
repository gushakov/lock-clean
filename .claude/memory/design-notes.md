# lock-clean — design notes

> **Charter — rationale, history, open threads. The *why*, not the *what*.**
> This file owns the narrative: design decisions and their motivations,
> methodology assessment, open architectural threads, Q&A captured from
> sessions, comparisons to reference projects, and pointers to ADRs (if the
> project maintains `doc/decisions/`).
>
> - Update when a decision is made and its *why* is worth remembering — or
>   when an open thread is opened, closed, or revised.
> - Current facts (what something is, where it lives, which class owns it)
>   belong in `project-context.md` or `project-context-extended.md`. If you
>   catch yourself writing a fact table here, move it.
> - Pointers to reference files (§-ref style) are welcome so this file can
>   stay focused on reasoning while still being navigable.

## Project overview

`lock-clean` exists to make one point concrete: in a Clean DDD application whose
aggregates are small and reference each other by id, an invariant that spans several
aggregates (a course's subscriber count against its capacity, a student's subscription
count against a limit) cannot be protected by any single aggregate's constructor. The
project shows the optimistic-locking answer — re-save the aggregates whose state the
decision depended on, inside the same transaction as the new one, so a concurrent
writer who touched any of them loses on a version check — and makes the race
reproducible. It is the companion code to a public Medium article, a tutorial rather
than a product, exercised through an integration test instead of a UI.

The article was prompted by the *Dynamic Consistency Boundary* (DCB) talk by Sara
Pellegrini and Milan Savić (DDD France), which addresses inter-aggregate invariants for
event-sourced aggregates through tagged "pure events" and conditional appends against
the stream query a command handler decided on. The article's thesis is that Clean DDD's
**use case consistency boundary** plays the same role for state-based aggregates: the
use case issues the read-model queries (the "stream query"), checks the rules, and
writes inside a tight transaction; the optimistic-lock version check is the state-based
analogue of the conditional append. The aggregate partition shown here is the
author's, not the talk's.

## Doctrine captured from the article

- **Invariants deliberately live in the use case, not in an aggregate.** The three
  rules are inter-aggregate counts; no single aggregate owns them. The use case reads
  like the user story — query, check, write — which the talk identifies as what
  aggregate-centric designs tend to obscure.
- **The super-aggregate is rejected on efficiency.** A `Faculty` holding every
  subscription would be "pure" and "complete" (Khorikov's trilemma) but would serialize
  every subscription against every other, including unrelated student/course pairs.
  Three independent aggregates are kept and contention is handled at the use-case
  boundary instead.
- **Why `Course` and `Student` are re-saved unchanged.** Writing only the new
  `Subscription` involves no version check at all (the row is new), so two concurrent
  runs can both pass the capacity check and both insert. Re-saving the aggregates the
  decision rested on turns their `@Version` into the lock token. Rule 2 ("capacity can
  change at any time") is the reason the course itself, not just its subscription
  count, is part of the decision state.
- **Accepted drawback.** The re-save makes the subscription lose to *any* concurrent
  write on the course or student, including an unrelated one (a title edit). The
  article accepts this on the Clean DDD bet that different use cases are run by
  different actors at different times, so such overlap is rare — the same
  actor/temporal/instance partitioning argument as `persistence-and-transactions.md`
  §TOCTOU.
- **The actor is a batch system.** `StudentId` is a use-case argument rather than
  taken from an authenticated principal precisely so the use case can be run
  concurrently for many students; that framing shapes the lost-race reaction (warn,
  let the batch retry) — see §Transaction demarcation below.

## Transaction demarcation — the lean port, applied to a three-write transaction

The transaction port and adapter were ported from `game-clean` (issue #1), replacing
the project's original wide port (`rollback()`, `doAfterRollback`, `*WithResult`
after-hooks over `afterCompletion(int)`). The doctrine is in
`persistence-and-transactions.md` §`TransactionOperationsOutputPort`, §The after-commit
hook must fail loudly, §Where the lock-conflict type lives; what follows is how it was
applied *here* and what this project adds to it.

- **Why the original design presented twice.** The article's use case caught the lock
  conflict *inside* the `doInTransaction` lambda, registered a `doAfterRollback`
  presentation, and returned normally. But the joined `@Transactional` gateway call had
  already marked the transaction rollback-only, so whoever owned the outermost
  transaction later got `UnexpectedRollbackException` on commit. The article's own log
  shows it surfacing from the test's transaction listener (the `@DataJdbcTest` slice owned
  the outermost transaction); in production the use case's template would have been the
  outermost, and the exception would have reached `presentError` *after* the rollback
  presentation. The lean port removes the hazard structurally: the error propagates out
  of the lambda, the template rolls back explicitly, and exactly one of handler /
  outermost catch presents.
- **The handler is per transaction, and here that is the right grain.** The
  methodology's "contested → handler" rule was formulated for a single write that can
  lose. This transaction holds three versioned writes, any of which may lose. The
  reaction is nonetheless one: "the state this decision rested on is stale — stop and
  warn." No re-read to name the winner is wanted, because the actor is a batch, not
  someone mid-edit; the batch retries. So one `onLockDetected` covers all three saves,
  and the lambda's short-circuit at the losing save is a feature (the IT asserts the
  student is never written when the course save loses).
- **A lost race is a warning, not an error.** The presenter method was renamed from
  `presentErrorOn…` to `presentWarningIf…`, aligning it with the other two outcome
  stripes (capacity, limit): all three are "the rules, as the world now stands, refuse
  this subscription." Only genuine faults ride `presentError`.
- **`@Transactional` dropped from the gateway.** Spring Data's `SimpleJdbcRepository` is
  itself transactional, so every repository call still runs in its own transaction or
  joins the use case's — the floor the methodology's floor/ceiling argument needs is
  still there, one level down. What is given up is atomicity of a hypothetical
  multi-repository gateway method called *outside* `doInTransaction`, which is exactly
  the crutch the doctrine says the use case must not lean on. The gateway's "POINT OF
  INTEREST" now says so.
- **The IT runs without a test-managed transaction.** The `onLockDetected` ordering
  ("handler runs after the rollback") holds only when the use case's transaction is the
  outermost. Under a `@DataJdbcTest` slice it is a participant, the handler would run
  before any real rollback, and the test would die at commit as the article's log shows.
  `@SpringBootTest` with no test transaction makes the use case's demarcation the real
  one, so the IT can assert what the article could only narrate: one presentation,
  rollback observable (no subscription row), versions moved only by the winner.
- **The rival write is deterministic.** Rather than two threads with latches or the
  article's 30-second pause, the IT decorates the real persistence port so the rival
  re-save of the course runs just before the use case's last read. Single-threaded,
  faithful to the article's interleaving (course read and rule checked, *then* the
  rival moves it), and repeatable. The manual experiment is retired; the recipe in
  `project-context-extended.md` says how to recreate it if a reader wants to.
- **The register use cases keep the propagating default, and say so in a test.** A
  fresh aggregate has no version to lose a race on. Per the methodology, the decline is
  recorded and pinned: an `OptimisticLockingError` reaching that write arrives at
  `presentError` as itself, with no named outcome.
- **A minimal main class exists for one reason.** `@SpringBootTest` needs a
  `@SpringBootConfiguration` and the slice's auto-configuration import is gone.
  `LockCleanApplication` sits at the `infrastructure` root (scan never reaches `core`),
  and the application has no driving adapter: it is a context, not a program. Use cases
  stay plain objects assembled by whoever drives them.

## Methodology assessment

After the port, the code conforms to `persistence-and-transactions.md` on every point
it covers: lean port with the lock-detect overload, `afterCommit()` fail-loud hook,
narrow transaction-error translation, `OptimisticLockingError` on neutral ground as a
sibling of the port errors, reads and rule checks outside the transaction, one narrow
write transaction, deferred success, single outermost checkpoint. The remaining
deviations (`project-context-extended.md` §Non-obvious conventions) are naming drift
from the reference projects (`port/db`, `PersistenceGateway`, `obtain*`,
`PersistenceOperationError`) — left as-is because the article quotes these names, and
renaming would break the correspondence between the public text and the code — plus
the absence of an ArchUnit guard and of a composition root.

## Open threads

1. Whether the duplicate-subscription guard should also exist as a database unique
   constraint on `(student_id, course_id)`, making the read-then-insert race a
   constraint violation rather than relying on the aggregate re-saves alone. (The
   re-saves already cover it: two concurrent duplicate attempts both re-save the same
   course and student, so one loses. A constraint would be belt-and-braces and would
   introduce a *second* concurrency detector, the case the methodology defers the
   `ConcurrencyConflictError` parent hierarchy to.)
2. An ArchUnit guard for `core ↛ infrastructure` and for the transaction adapter not
   depending on `core.port.db` (pinning the neutral `port/concurrency` placement).

## Comparisons to reference projects

- **`hotel-clean`** — same narrow-transaction / deferred-presentation shape; this
  project adds the multi-aggregate lock technique on top of it.
- **`game-clean`** — the source of the lean transaction port, the fail-loud after-commit
  adapter, its unit test over `NoOpTransactionManager`, and the lock-detect idiom. Its
  `AnnounceTimeOfDayUseCase` is the single-write handler reference; `MoveUseCase` the
  single-writer propagating reference. lock-clean extends the idiom to a three-write
  transaction and shows the one-handler-per-transaction grain.

## ADR pointer

This project does not currently maintain a `doc/decisions/` directory. If formal ADRs
are introduced, link them from here.
