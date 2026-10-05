Optimistic locking with Clean DDD
---

This project showcases how optimistic locking may be used for a set of aggregates in a Clean DDD application. It
explores specifically a scenario with multi-thread concurrency.

There is a
Medium [article](https://medium.com/unil-ci-software-engineering/optimistic-concurrency-locking-and-inter-aggregate-invariants-in-clean-ddd-9a7adcbb7bbe)
which discusses the relevant concepts in more detail.

### Running tests

Requires JDK 21 on `JAVA_HOME` and Maven.

```
mvn test
```

runs the unit tests: the domain construction gate, the use case interaction tests against mocked ports, and
`SpringTransactionAdapterTest`, which pins the transaction adapter's semantics (narrow error translation, the
optimistic-lock handler, and the fail-loud after-commit hook) over a minimal real transaction manager.

There is `docker-compose.yaml` which will start a local Postgres database. With it running,

```
mvn test -Dtest=SubscribeStudentIT
```

runs the integration test which shows how `Course`, `Student`, and `Subscription` aggregates are created and
modified during the execution of the use cases — and how a subscription loses to a concurrent modification of
the course (made deterministic by a decorated persistence port), is rolled back, and is presented exactly once as
a warning.
