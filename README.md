Optimistic locking with Clean DDD
---

This project showcases how optimistic locking may be used for a set of aggregates in a Clean DDD application. It
explores specifically a scenario with multi-thread concurrency.

There is a
Medium [article](https://medium.com/unil-ci-software-engineering/optimistic-concurrency-locking-and-inter-aggregate-invariants-in-clean-ddd-9a7adcbb7bbe)
which discusses the relevant concepts in more detail.

### Running tests

There is `docker-compose.yaml` which will start a local Postgres database.
There are integration tests in `SubscribeStudentTestIT.java` which can be run to see how `Course`, `Student`, and
`Subscription` aggregates are created and modified during the execution of use cases.