# 🏥 Patient Management Pro (Microservices Backend)

![Java](https://img.shields.io/badge/Java-17-blue.svg) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg) ![Microservices](https://img.shields.io/badge/Architecture-Microservices-orange.svg) ![Kafka](https://img.shields.io/badge/Apache%20Kafka-Event%20Driven-black.svg) ![gRPC](https://img.shields.io/badge/gRPC-RPC-blueviolet.svg) ![Docker](https://img.shields.io/badge/Docker-Containerized-blue.svg)

> A production-ready, distributed microservices backend for patient lifecycle management. Built with modern engineering practices including secure edge APIs, asynchronous event messaging, synchronous gRPC communication, robust caching, deep observability, and complete CI/CD automation.

---

## 🌟 Engineering Highlights

This project is carefully crafted to demonstrate enterprise-grade backend engineering, making it an excellent showcase for technical recruiters and open-source contributors alike.

- **Distributed Architecture:** 5 highly-decoupled microservices (`api-gateway`, `auth`, `patient`, `billing`, `analytics`) with independent databases and strict bounded contexts.
- **Mixed Communication Models:** REST at the edge, **gRPC** + Protobuf for high-speed internal synchronous calls, and **Apache Kafka** for scalable, asynchronous event-driven processing.
- **Enterprise Security & Reliability:** Stateless JWT authentication, API Gateway edge routing, Redis-backed rate limiting, and Resilience4j circuit breaker patterns.
- **Deep Observability:** Complete monitoring pipeline exposing JVM and application metrics via Micrometer, scraped by **Prometheus**, and visualized in **Grafana**.
- **Platform Automation:** Fully containerized environment spinning up multiple interconnected containers via `docker-compose`. Backed by automated GitHub Actions and Jenkins CI/CD pipelines.
- **Quality Assurance:** Covered by automated end-to-end integration tests (`integration-tests/`) and rigorous `k6` load testing scenarios (`performance-test/`).

---

## ⚡ Quick Start Demo (7 Minutes)

A streamlined guide to bringing the platform up, validating the core flows, and testing the observability stack.

### 1) Start the platform

```powershell
Copy-Item .env.example .env
docker compose up --build -d
```

### 2) Login and capture JWT token

Open `api-request/auth-service/login.http` and run it from the IDE HTTP client.

- Endpoint: `POST http://localhost:4004/auth/login`
- Token is stored as `{{token}}` by the request script

### 3) Call protected APIs

Run these files in order:

1. `api-request/patient-service/create-patient.http`
2. `api-request/patient-service/get-patients.http`

If you want to validate the token directly, run `api-request/auth-service/validate.http`.

### 4) (Optional) Check observability

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

### 5) Stop the platform

```powershell
docker compose down
```

## Architecture

```mermaid
flowchart TD
    classDef microservice fill:#e1f5fe,stroke:#0288d1,stroke-width:2px,color:#000;
    classDef database fill:#fff3e0,stroke:#f57c00,stroke-width:2px,color:#000;
    classDef messaging fill:#e8f5e9,stroke:#388e3c,stroke-width:2px,color:#000;
    classDef cache fill:#fce4ec,stroke:#c2185b,stroke-width:2px,color:#000;
    classDef observability fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#000;
    classDef client fill:#eceff1,stroke:#455a64,stroke-width:2px,color:#000;

    Client(("📱 Client")):::client

    subgraph Edge ["🌐 Edge Layer"]
        Gateway["🚦 API Gateway"]:::microservice
    end

    subgraph Core ["⚙️ Core Services"]
        Auth["🔒 Auth Service"]:::microservice
        Patient["🏥 Patient Service"]:::microservice
        Billing["💳 Billing Service"]:::microservice
        Analytics["📊 Analytics Service"]:::microservice
    end

    subgraph Data ["💾 Data Layer"]
        ADB[("Auth DB")]:::database
        PDB[("Patient DB")]:::database
        Redis[("Redis")]:::cache
        Kafka[/"Apache Kafka"/]:::messaging
    end

    subgraph Obs ["📈 Observability"]
        Prom["Prometheus"]:::observability
        Grafana["Grafana"]:::observability
    end

    Client ==>|HTTPS REST| Gateway
    Gateway -->|Validates JWT| Auth
    Gateway ==>|Routes Request| Patient

    Auth -.->|Read/Write| ADB
    Patient -.->|Read/Write| PDB
    Gateway -.->|Rate Limit Check| Redis
    Patient -.->|Cache| Redis

    Patient ==>|gRPC Sync| Billing
    Patient -->|Publishes Event| Kafka
    Kafka -->|Consumes Event| Analytics

    Patient -.->|Actuator Metrics| Prom
    Prom -.->|Visualise| Grafana
```

### Communication patterns

| Pattern | Where used |
|---|---|
| REST/HTTP | Client -> API Gateway -> Auth/Patient services |
| gRPC | Patient Service -> Billing Service |
| Kafka | Patient Service -> Analytics Service |

## Technology Choices: Why & How

### Spring Boot 3.x & Java
* **Why:** Industry standard for enterprise microservices. It offers a massive ecosystem, fast development via auto-configuration, and robust community and corporate support.
* **How:** Used as the foundational framework across all 5 microservices (`api-gateway`, `auth`, `patient`, `billing`, `analytics`), with Java 17 for the backend services. Each service runs independently with its embedded Tomcat/Netty server.

### Spring Cloud Gateway
* **Why:** Provides a simple, effective way to route traffic to underlying APIs while providing cross-cutting concerns (security, monitoring, resiliency) at the edge.
* **How:** Implemented in `api-gateway`. It acts as the single entry point, routing requests to `auth-service` and `patient-service`, and applies Redis-backed request rate limiting to prevent abuse.

### JWT (JSON Web Tokens) & Spring Security
* **Why:** Enables stateless authentication. In a distributed backend, maintaining server-side sessions across multiple instances or hitting a central DB for every request validation is a massive bottleneck.
* **How:** The `auth-service` generates a cryptographically sealed JWT upon correct login. The `api-gateway` and downstream services validate the token locally without needing to query a database.

### Apache Kafka & Event-Driven Architecture
* **Why:** To decouple the critical write path from downstream processes. This ensures core APIs remain highly available and fast even if downstream services (like analytics) are slow or offline.
* **How:** `patient-service` acts as a producer, publishing event messages (e.g., `PatientCreatedEvent`) to a Kafka topic. The `analytics-service` consumes these topics to process metrics asynchronously, ensuring eventual consistency.

### gRPC & Protocol Buffers
* **Why:** We need high-performance, strictly typed internal communication between services. gRPC uses HTTP/2 and binary Protobufs, which offer smaller payloads and significantly faster serialization than JSON over REST.
* **How:** Used for synchronous inter-service communication where an immediate response is required. The `patient-service` calls the `billing-service` (gRPC server) to immediately provision/check billing accounts during patient onboarding.

### PostgreSQL (Per-Service Database)
* **Why:** Using the "Database-per-service" pattern ensures loose coupling. If one service's schema changes, or its database is under load, other services are entirely unaffected.
* **How:** `auth-service` and `patient-service` each have isolated, separate PostgreSQL containers orchestrated via Docker (`auth-service-db`, `patient-service-db`). They interact via Spring Data JPA.

### Redis
* **Why:** Excellent for ultra-fast, in-memory data storage to avoid repeating heavy DB queries, and perfectly suited for atomic distributed operations like counting.
* **How:** Used dynamically for two purposes: 
  1. **Rate Limiting:** `api-gateway` uses Redis to track API request limits per client IP.
  2. **Caching:** `patient-service` uses Spring's `@Cacheable` to cache retrieval endpoints, drastically dropping latency for frequent queries.

### Prometheus & Grafana (Observability)
* **Why:** You cannot optimize or debug what you cannot see. In distributed systems, pinpointing failures or performance bottlenecks requires centralized metric collection.
* **How:** We expose `/actuator/prometheus` endpoints in our microservices using Micrometer. Prometheus periodically scrapes these endpoints. Grafana connects to Prometheus to render beautiful, actionable dashboards for request latency, errors, and JVM metrics.

### Docker & Docker Compose
* **Why:** To eliminate "it works on my machine" issues and ensure environments are reproducible. Containerization guarantees that the code runs precisely the same way in development, testing, and production.
* **How:** Every service has a `Dockerfile`. The entire multi-service ecosystem (5 services, 2 DBs, Redis, Kafka, Prometheus, Grafana) spins up cleanly with a single `docker compose up` command.

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17 (services), Java 21 (infrastructure module) |
| Framework | Spring Boot 3.x |
| Gateway | Spring Cloud Gateway |
| Security | Spring Security + JWT (`jjwt`) |
| Messaging | Apache Kafka + Protocol Buffers |
| RPC | gRPC (`grpc-spring-boot-starter`) |
| Datastore | PostgreSQL |
| Cache / Rate limit support | Redis |
| Resilience | Resilience4j |
| Observability | Actuator + Micrometer + Prometheus + Grafana |
| Testing | JUnit 5, RestAssured, k6 |
| Containers | Docker, Docker Compose |
| IaC | AWS CDK (in `infrastructure/`) |
| CI/CD | GitHub Actions + Jenkins pipeline |

## 📈 Engineering Impact

| Area | What Was Built | Why It Matters |
|---|---|---|
| 🏗️ **Architecture** | 5-service distributed backend with an API Gateway, per-service PostgreSQL DBs, and strict bounded contexts | Demonstrates production-grade system design with independent deployability and zero shared state |
| 🔒 **Security** | Stateless JWT auth via `auth-service`; all downstream routes protected through the API Gateway | Eliminates session-state bottlenecks in distributed systems; scales horizontally without sticky sessions |
| ⚡ **Inter-Service Comms** | gRPC + Protobuf for synchronous calls; Apache Kafka for async event-driven messaging | Mixed communication model shows real-world trade-off analysis — speed vs. decoupling |
| 🚀 **Performance** | Redis caching on hot endpoints + Redis-backed rate limiting at the gateway | Protects downstream services under traffic spikes; drastically reduces DB query load |
| 🔭 **Observability** | Micrometer → Prometheus scrape pipeline + Grafana dashboards for JVM and HTTP metrics | Full production visibility into latency, errors, and throughput without a third-party APM |
| 🧪 **Quality** | RestAssured integration tests + k6 load tests targeting 600 VUs | Validates distributed behavior end-to-end and measures system breaking points under stress |
| 🤖 **CI/CD** | GitHub Actions matrix build + Jenkins declarative pipeline + Docker Compose orchestration | Automated from commit to tested deployment; mirrors real team engineering workflows |

## Local Setup

### Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin)
- Java 17 (for service builds/tests)
- Maven 3.9+ (or use each module's Maven wrapper)
- Optional: k6 (for load testing)

### Environment variables

Create `.env` from `.env.example`:

```dotenv
JWT_SECRET=Your_JWT_Secret_Key
POSTGRES_USER=Db_User
POSTGRES_PASSWORD=Db_Password
```

```powershell
Copy-Item .env.example .env
```

### Run all services

```powershell
docker compose up --build -d
```

### Stop services

```powershell
docker compose down
```

### Runtime ports

| Component | Port(s) |
|---|---|
| API Gateway | `4004` |
| Auth Service | `4005` |
| Patient Service | `4000` |
| Billing Service | `4001` (HTTP), `9001` (gRPC) |
| Analytics Service | `4002` |
| Auth DB (Postgres) | `5001` |
| Patient DB (Postgres) | `5000` |
| Redis | `6379` |
| Kafka | `9092` |
| Prometheus | `9090` |
| Grafana | `3000` |

## API Docs and Request Collections

### Swagger

- Patient Service: `http://localhost:4000/swagger-ui/index.html`
- Auth Service: `http://localhost:4005/swagger-ui/index.html`

### Request files

- Auth: `api-request/auth-service/login.http`, `api-request/auth-service/validate.http`
- Patient: `api-request/patient-service/create-patient.http`, `api-request/patient-service/get-patients.http`, `api-request/patient-service/update-patient.http`, `api-request/patient-service/delete-patient.http`
- gRPC sample: `grpc-request/billing-service/create-billing-account.http`

## Testing

### Build services (skip tests)

```bash
cd auth-service && ./mvnw clean install -DskipTests
cd ../patient-service && ./mvnw clean install -DskipTests
cd ../billing-service && ./mvnw clean install -DskipTests
cd ../analytics-service && ./mvnw clean install -DskipTests
cd ../api-gateway && ./mvnw clean install -DskipTests
```

### Integration tests

Start services first, then run:

```bash
cd integration-tests
mvn test
```

### Performance test (k6)

```bash
k6 run performance-test/patient-test.js
```

## Observability

- Prometheus config: `monitoring/prometheus.yml`
- Scrape interval: 5 seconds
- Current scrape target: `patient-service:4000/actuator/prometheus`
- Grafana URL: `http://localhost:3000`

## CI/CD

### GitHub Actions (`.github/workflows`)

- `maven.yml` - matrix build for all services on `push`/`pull_request` to `main`
- `docker-build.yml` - Docker image builds on `push` to `main`
- `integration-test.yml` - Compose up -> integration tests -> teardown

### Jenkins (`Jenkinsfile`)

Pipeline stages:

1. Fix Maven wrapper execute permissions
2. Build each service (`-DskipTests`)
3. Build Docker Compose images
4. Start the full platform
5. Run `integration-tests`
6. Stop the platform

## Service Deep Dive

### `api-gateway`

- Spring Cloud Gateway entry point
- Routes requests to backend services
- Includes reactive Redis dependency for rate limiting support

### `auth-service`

- Authentication and JWT token operations
- Spring Security + JPA + PostgreSQL
- Swagger/OpenAPI enabled

### `patient-service`

- Core patient CRUD APIs
- Publishes events to Kafka
- Calls `billing-service` via gRPC
- Uses Redis caching and Micrometer metrics
- Includes Flyway and Resilience4j

### `billing-service`

- gRPC billing account endpoint
- Protobuf + gRPC code generation via Maven

### `analytics-service`

- Consumes Kafka events
- Processes Protobuf payloads

## Repository Layout (Complete)

| Path | Purpose |
|---|---|
| `api-gateway/` | Gateway service for routing and edge concerns |
| `auth-service/` | Authentication service and JWT APIs |
| `patient-service/` | Core patient domain service |
| `billing-service/` | gRPC billing service |
| `analytics-service/` | Kafka consumer/analytics service |
| `integration-tests/` | End-to-end tests (RestAssured + JUnit) |
| `performance-test/` | k6 load testing script (`patient-test.js`) |
| `monitoring/` | Prometheus Dockerfile and scrape config |
| `infrastructure/` | AWS CDK code + LocalStack deploy helper script |
| `api-request/` | HTTP request collections for auth and patient APIs |
| `grpc-request/` | gRPC request samples for billing service |
| `k8/` | Kubernetes manifests folder (currently empty placeholder) |
| `Db_volumes/` | Local persisted Postgres volume data |
| `.github/workflows/` | GitHub Actions CI workflows |
| `docker-compose.yml` | Local full-system orchestration |
| `Jenkinsfile` | Jenkins declarative pipeline |

## Infrastructure

The `infrastructure/` module contains AWS CDK dependencies and generated CloudFormation templates in `infrastructure/cdk.out/`.

`infrastructure/localstack-deploy.sh` deploys the generated template to LocalStack and prints the load balancer DNS. The script is Bash-based, so run it in a Bash-compatible shell.

## Known Notes

- `k8/` is present but currently empty
- `Db_volumes/` contains local database volume data and is environment-specific
- Some request collection files point to LocalStack load balancer URLs; update endpoints for your local run mode if needed

## 🛠️ Proof of Work (Evidence-Based Impact)

- **Complete Local Platform:** The system runs flawlessly via a single `docker compose up` command, provisioning all microservices, per-service PostgreSQL databases, Kafka brokers, Redis clusters, and the entire Prometheus/Grafana monitoring stack.
- **Load Tested & Hardened:** The custom load-test script (`performance-test/patient-test.js`) executes a high-stress scenario scaling up to 600 Virtual Users (VUs) with strict latency and failure thresholds to guarantee resilience.
- **Integration Test Coverage:** The dedicated `integration-tests/` module leverages RestAssured to validate end-to-end behavioral flows across the deployed multi-container stack.
- **Automated CI/CD Pipeline:** Deep CI coverage is demonstrated in both `.github/workflows/` (GitHub Actions) and `Jenkinsfile` (Jenkins Declarative Pipeline), showcasing industry-standard build, test, and containerization automation.
- **Live Observability:** The monitoring data path is actively wired through `monitoring/prometheus.yml` and live Spring Boot Actuator endpoints, providing immediate insight into platform health.

---

## 🤝 Let's Connect

**Rinit Bhowmick**

[LinkedIn](https://linkedin.com/in/rinit-bhowmick)  
[GitHub](https://github.com/rinit18)
