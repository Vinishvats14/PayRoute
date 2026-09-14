PayRoute — Architecture & explanation

Purpose

This document expands the short "note" into conceptual reasoning: why each design choice exists, how to validate it, and what to check when confused.

1) Java 21
- Why: modern language improvements (records, pattern matching) reduce boilerplate and express DTOs clearly.
- How to verify: records used under dto/ packages; compile uses java.version 17/21 in pom.

2) Spring Boot
- Why: quick microservice setup, autoconfiguration and opinionated tooling.
- Verify: each module is a Spring Boot app (Application class under each service).

3) Spring Security + JWT
- Why: stateless, scalable auth for APIs. JWT carries user identity across services.
- Pitfalls: leaking secrets, expired tokens. Verify: JwtService and JwtConfig exist; tests use test properties.

4) PostgreSQL (prod) and H2 (tests)
- Why: Postgres for production features; H2 for fast CI tests.
- Verify: module pom files include postgres runtime dependency; test resources contain H2 settings.

5) JPA/Hibernate
- Why: productive ORM for relational mapping and transactions.
- Verify: @Entity classes in entity/ and repositories with Spring Data JPA.

6) BigDecimal for money
- Why: avoid rounding errors from float/double.
- Verify: entity fields use BigDecimal and arithmetic uses compareTo/add/subtract.

7) Database transactions
- Why: atomicity across multiple DB changes (transaction + ledger entries).
- Verify: @Transactional on transfer service methods; save operations happen within transaction.

8) Pessimistic row locking
- Why: prevent concurrent transfers from corrupting balances; acquire locks in deterministic order to avoid deadlocks.
- Verify: repository has findByIdForUpdate or similar; code orders wallet ID locking by ID.

9) Optimistic versioning
- Why: elsewhere where optimistic concurrency is desired; @Version fields detect concurrent updates.
- Verify: @Version in entities.

10) Idempotency keys
- Why: allow safe retries from clients — repeated requests with same idempotency key should return the original result.
- Verify: unique constraint on idempotency_key and repository lookup before creating transactions.

11) DB unique constraints
- Why: enforce invariants at DB level (idempotency keys, unique indexes).
- Verify: check @Table(uniqueConstraints = ...).

12) Double-entry ledger
- Why: accounting invariant — for every completed transfer, DEBIT == CREDIT. This is the single most important invariant.
- Verify: ledger_entries contain two lines per transaction; unit tests should assert sums match.

13) Transaction states
- Why: hold PENDING while moving money and creating ledger entries; mark COMPLETED only after everything succeeds.
- Verify: Transaction.status transitions PENDING -> COMPLETED.

14) Atomic wallet transfers
- Why: combine balance mutation and ledger creation in one DB transaction so partially applied operations do not occur.
- Verify: method is @Transactional and performs all steps before commit.

15) Concurrency/deadlock awareness
- Why: avoid deadlocks by locking wallets in ascending ID order.
- Verify: code compares senderWalletId and receiverWalletId and locks in that order.

Quick debugging checklist

- Build and tests: ./mvnw -B test
- Reproduce compile error: run module build: ./mvnw -pl wallet-service -DskipTests=true package
- If token issues: check jwt.secret in application.properties or test resources
- If DB issues during tests: ensure H2 test properties exist under src/test/resources
- To check invariant: query ledger_entries grouped by transaction_id and verify debit sum == credit sum for completed transactions

