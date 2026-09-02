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

Next improvements (ideas)

- Extract shared types (JwtConfig, User, Role) into a common "common" module used by microservices
- Replace placeholder userId with actual extraction from SecurityContext
- Add integration tests for transfer flows using Testcontainers with Postgres
- Add metrics/alerts for idempotency collisions and rate limiting

If you want, next actions I can take:
- Create README.md and ARCHITECTURE.md (done)
- Move shared classes into a new common module
- Implement userId extraction from security context in TransactionService (recommended)
- Add a quick invariant test that fails if debit != credit

Which next action should be done? (pick one)

Good — below is a compact, interview-ready explanation + common junior-level questions and short model answers focused on the idempotency note you shared.
Core concept (short)
•
Idempotency: making repeated identical requests produce the same effect as one request. For POST transfers, clients send an idempotency_key so retries don’t create duplicate transactions.
•
Why DB UNIQUE is required: app-level findByIdempotencyKey() can show “not found” concurrently for two requests — only a UNIQUE constraint prevents both from persisting duplicates when both commit.
How it should be implemented (steps)
1.
Client sends idempotency_key with request header/body.
2.
Server checks repository.findByIdempotencyKey(key):
◦
If present → return stored response (do not re-process).
3.
If not present → attempt to create transaction inside a DB transaction.
4.
Rely on DB UNIQUE(idempotency_key). If concurrent creates race, one insert succeeds; the other fails with duplicate-key.
5.
Catch duplicate-key exception (DataIntegrityViolationException), then load existing record and return it.
6.
Optionally store response metadata (status, result, timestamp) so repeated calls return identical responses.
Pseudocode (conceptual)
•
existing = repo.findByIdempotencyKey(key)
•
if existing: return existingResponse
•
try:     save new transaction (within @Transactional)     process transfer, ledger entries     save final transaction state & response     return response   catch DuplicateKeyException:     return repo.findByIdempotencyKey(key)  // someone else won the race
Testing & verification
•
Unit: simulate two parallel requests with same key; assert only one transaction persisted.
•
DB: verify unique constraint exists on idempotency_key.
•
Integration: retry behavior and that response is deterministic.
Common pitfalls
•
Not storing the response/result → second caller can’t return same output.
•
Expiring keys too soon (clients retried after expiry create duplicates).
•
Treating idempotency key as auth token — must still validate caller identity.
•
Partial failures: store state (PENDING/COMPLETED) so retries inspect state.
Interview Qs (fresher / 0–1 yr) + short answers
1)
What is idempotency?
•
Making repeated identical operations produce one effect; use idempotency_key for POST retries.
2)
Why do you need a UNIQUE constraint if you check in code first?
•
Concurrent requests can both see “not found”; DB uniqueness is the final guard.
3)
How do you handle a duplicate-key error at runtime?
•
Catch DB unique constraint exception, query by idempotency_key and return stored result.
4)
What do you store with the idempotency key?
•
Transaction record + final status + response payload/timestamp and caller id for validation.
5)
When should an endpoint be idempotent?
•
Any non-idempotent action clients may retry (payments, transfers). GET/PUT/DELETE have different semantics.
6)
How long should keys live?
•
Depends on retry patterns; keep them long enough for client retries (minutes/hours) then TTL can be applied.
7)
Is idempotency the same as transaction isolation?
•
No. Idempotency prevents duplicate business actions; isolation ensures read/write consistency. Both help but solve different problems.
8)
How to prevent a different user from reusing someone else’s key?
•
Store key + caller id; validate caller identity before returning stored result.
9)
What if response payload is large/contains secrets?
•
Store a reference or hashed payload; do not expose sensitive data; return minimal consistent result.
10)
How to test on CI?
•
Use parallel threads/requests to same endpoint with same key and assert a single DB row and consistent response.
If you want, next I can:
•
Add a docs/IDEMPOTENCY.md with the above plus example tests and a snippet you can paste into a service.
•
Implement the duplicate-key catch + response-reuse helper in TransactionService (short, safe change).
Which of those two would you like now?
gpt-5