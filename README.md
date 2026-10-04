# Multi-Agent Real-Time Fraud Triage & Compliance Audit Engine

Portfolio implementation of the supplied system design.

## Architecture

Client -> Spring Boot Gateway -> MySQL
                         |
                         +-> Python FastAPI + LangGraph -> local policy RAG

Spring Boot owns persistence and security. Python never writes to MySQL; it returns a structured verdict and the gateway persists the audit trail.

## What is implemented

- Spring Boot 3.3 / Java 17
- JWT authentication with analyst/admin roles
- Transaction persistence with MySQL/JPA
- Cheap pre-filter for amount, velocity and geo-risk
- REST call from Java to Python
- Resilient timeout/fallback handling
- FastAPI + LangGraph workflow:
  - Pattern Analyst
  - Compliance Auditor
  - Conditional human escalation
  - Decision Agent
- Local policy RAG using TF-IDF cosine similarity; no API key required
- Per-agent audit records
- Final audit log
- Admin-only override endpoint
- Unit tests on both services
- Docker Compose
- Health endpoints
- Synthetic data only

## Run

Prerequisites: Docker Desktop.

```bash
docker compose up --build
```

Then:

- Gateway: http://localhost:8080
- Agent service: http://localhost:8000
- Swagger: http://localhost:8000/docs

### Get a demo JWT

```bash
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Use the returned token:

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "externalRef":"TX-00042",
    "accountId":"ACC-9981",
    "amount":14500.00,
    "currency":"USD",
    "merchant":"Global Traders LLC",
    "country":"AE",
    "ipAddress":"203.0.113.7"
  }'
```

Expected outcome is normally `FREEZE`, because the transaction is above the synthetic high-value threshold (contributing to the Pattern Analyst score) and the destination country is a designated high-risk jurisdiction in the policy corpus (contributing to both the Pattern Analyst and Compliance Auditor scores). Together those push the combined risk score to 0.80, above the 0.70 freeze threshold in `risk_scorer`.

### Useful endpoints

- `POST /api/v1/auth/token`
- `POST /api/v1/transactions`
- `GET /api/v1/transactions/{id}`
- `GET /api/v1/transactions/{id}/audit-trail`
- `GET /api/v1/transactions?status=MANUAL_REVIEW`
- `POST /api/v1/transactions/{id}/override`
- `GET /actuator/health`

## Demo users

- `analyst / analyst123` -> ANALYST
- `admin / admin123` -> ADMIN

These credentials are intentionally demo-only.

The override endpoint (`POST /api/v1/transactions/{id}/override`) requires the `ADMIN` role;
an `ANALYST` token gets a 403. The `status` field in the override request body must be one of
the persistence-layer status names: `APPROVED`, `FROZEN`, `MANUAL_REVIEW`, or `PENDING` - these
are deliberately distinct from the agent-orchestrator's action vocabulary (`APPROVE` / `FREEZE`
/ `MANUAL_REVIEW`), which the gateway translates internally.

## Run tests

Java:

```bash
cd gateway-service
mvn test
```

Python:

```bash
cd agent-orchestrator
python -m venv .venv
# activate the venv
pip install -r requirements.txt
pytest
```

## Notes

The original design mentions Chroma/FAISS and an LLM. This implementation deliberately uses a deterministic TF-IDF policy retriever and deterministic scoring so the complete project works locally without an LLM API key. LangGraph is still used for orchestration. A production implementation can replace `PolicyRetriever` and the decision node with an embedding store and structured LLM call without changing the REST contract.

## Fixes applied in this pass

The project as supplied had a few defects that prevented it from actually working end to end:

- **Transaction submission crashed for most outcomes.** The agent-orchestrator returns
  `status` as `APPROVE` / `FREEZE` / `MANUAL_REVIEW`, but `TransactionStatus` only defines
  `APPROVED` / `FROZEN` / `MANUAL_REVIEW` / `PENDING`. The gateway called
  `TransactionStatus.valueOf(result.status())` directly, which threw `IllegalArgumentException`
  (surfaced as an HTTP 400) for any transaction that resolved to `APPROVE` or `FREEZE` -
  including the README's own example. Added `TransactionStatus.fromAgentAction(...)` to
  translate correctly, with a regression test (`TransactionStatusTest`).
- **The admin-only override endpoint had no authorization check.** Any authenticated user,
  analyst or admin, could call it. `SecurityConfig` now restricts
  `POST /api/v1/transactions/{id}/override` to `ROLE_ADMIN`.
- **No real timeout or fallback on the call to the agent-orchestrator**, despite that being
  listed as implemented. `OrchestratorClient` now sets explicit connect/read timeouts, and
  `TransactionService` catches orchestrator failures and falls back to `MANUAL_REVIEW` with an
  explanatory audit entry instead of losing the transaction's disposition or 500ing.
- Removed a dead no-op list comprehension left over in `graph.py`'s decision node.
