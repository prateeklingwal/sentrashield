# SentraShield — scaffold (step 1)

Bare-bones skeleton: Postgres + Spring Boot backend + FastAPI ml-service,
wired together on one Docker network. No auth, no ML, no LLM, no dashboard yet —
this step only proves the three containers start and can reach each other.

## Run

    docker compose up --build

## Verify

    curl http://localhost:8000/health
    curl http://localhost:8080/api/health
    curl http://localhost:8080/api/ping-ml
    curl http://localhost:8080/actuator/health
