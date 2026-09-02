PayRoute — microservices payment platform

Overview

PayRoute is a small Java microservices demo implementing safe, idempotent wallet transfers with strong invariants. It uses Spring Boot, Spring Security, JWTs, JPA and a double-entry ledger to ensure that completed transfers are balanced (DEBIT == CREDIT).

Quick start

- Build & test all modules: ./mvnw -B test
- Build a specific module: ./mvnw -pl transaction-service -am -DskipTests=true package

Resume-worthy features (short)

- Java 21 — modern language features and records
- Spring Boot — microservice framework
- Spring Security + JWT — stateless authentication
- PostgreSQL / H2 for tests — production DB + in-memory tests
- JPA/Hibernate — persistence layer
- BigDecimal for money — safe arithmetic
- Transactions & pessimistic row locking — atomic updates across wallets
- Optimistic versioning — prevents lost updates
- Idempotency keys — safe retries
- DB unique constraints — data integrity
- Double-entry ledger — debit/credit invariant
- Transaction states — PENDING/COMPLETED for safe processing
- Concurrency/deadlock awareness — correct locking order

Where to read more

See docs/ARCHITECTURE.md for conceptual explanations, reasons for each design choice, verification steps, and common pitfalls.
