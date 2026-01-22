# Paybase Test Task - Internal Balances API

## Run locally

### Requirements
- Java 17+
- Maven (or use `./mvnw`)

### Build & run
```bash
./mvnw spring-boot:run
```

### Docker
```bash
docker build -t paybase-test-task .
docker run -p 8080:8080 paybase-test-task
```

### Useful endpoints
- Health check: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Prometheus: `http://localhost:8080/actuator/prometheus`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Architecture overview

The project follows a layered Spring Boot design: controllers expose REST endpoints, services implement
transactional business rules, and repositories access the database via Spring Data JPA. Entities are
mapped to a relational schema managed by Flyway migrations. This keeps API concerns, domain logic, and
persistence clearly separated.

Each transaction stores before/after balances for auditability, while account balance is updated
atomically inside a single database transaction. Idempotency is enforced by a unique constraint on
`idempotency_key`, and repeated requests return the original transaction without changing balances.

## Concurrency control approach

Balance updates use pessimistic row-level locks (`PESSIMISTIC_WRITE`) to prevent concurrent withdrawals
or transfers from overspending. Transfers lock accounts in a stable ID order to reduce deadlock risk.
All balance changes happen within a single `@Transactional` boundary, so operations are atomic and
roll back on failure.

## Example curl requests

Create account:
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "merchant-001",
    "currency": "USD",
    "initialBalance": 0
  }'
```

Deposit:
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "deposit-001",
    "type": "DEPOSIT",
    "toAccountId": 1,
    "amount": 1000.00,
    "currency": "USD",
    "reference": "Initial deposit"
  }'
```

Transfer:
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "transfer-001",
    "type": "TRANSFER",
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 500.00,
    "currency": "USD",
    "reference": "Settlement"
  }'
```

Balance:
```bash
curl http://localhost:8080/api/accounts/1/balance
```

Statement:
```bash
curl "http://localhost:8080/api/accounts/1/statement?from=2026-01-01&to=2026-01-19"
```

Transaction details:
```bash
curl http://localhost:8080/api/transactions/1
```
