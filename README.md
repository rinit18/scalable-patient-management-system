# Scalable Patient Management System

A production-style microservices backend built with **Spring Boot**, **Apache Kafka**, and **gRPC**, demonstrating distributed system design, event-driven architecture, and infrastructure automation.

---

## Architecture
```
Client
   │
API Gateway (JWT validation + Redis rate limiting)
   │
   ├── Auth Service          Authentication & JWT issuance
   ├── Patient Service       Core domain logic (Kafka producer, Redis cache, metrics)
   ├── Billing Service       gRPC billing account provisioning
   └── Analytics Service     Kafka consumer for event processing
```

### Service Communication

| Pattern     | Usage                               |
|-------------|-------------------------------------|
| REST / HTTP | Client → API Gateway                |
| gRPC        | Patient Service → Billing Service   |
| Kafka       | Patient Service → Analytics Service |

---

## Tech Stack

| Category         | Technology                          |
|------------------|-------------------------------------|
| Language         | Java 17                             |
| Framework        | Spring Boot 3                       |
| Security         | Spring Security + JWT               |
| Gateway          | Spring Cloud Gateway                |
| Messaging        | Apache Kafka + Protocol Buffers     |
| RPC              | gRPC                                |
| Caching          | Redis                               |
| Database         | PostgreSQL                          |
| Resilience       | Resilience4j                        |
| Metrics          | Micrometer + Prometheus + Grafana   |
| Containerization | Docker + Docker Compose             |
| Infrastructure   | AWS CDK                             |
| CI/CD            | GitHub Actions                      |
| Testing          | JUnit 5 + RestAssured + k6          |

---

## Key Engineering Decisions

- **Separate PostgreSQL databases per service** — no shared state between microservices
- **Kafka with Protobuf serialization** — type-safe, schema-enforced async messaging
- **Reliable Kafka producer** (`acks=all`, retries) — ensures no event loss under failure
- **Resilience4j circuit breakers** — graceful degradation when billing service is unavailable
- **Redis rate limiting at gateway** — protects all downstream services from traffic spikes
- **Custom Micrometer AOP aspect** — tracks Redis cache miss ratio as a named metric
- **Multi-stage Docker builds** — optimized image sizes across all services
- **AWS CDK (IaC)** — infrastructure provisioned as code, not manually

---

## Getting Started

### Prerequisites
- Docker
- Docker Compose

### Run locally
```bash
# Copy environment variables
cp .env.example .env

# Start all services
docker-compose up --build
```

This starts all microservices, PostgreSQL databases, Kafka, and Redis.

### Service Ports

| Service           | Port                       |
|-------------------|----------------------------|
| API Gateway       | 4004                       |
| Auth Service      | 4005                       |
| Patient Service   | 4000                       |
| Billing Service   | 4001 (HTTP) / 9001 (gRPC)  |
| Analytics Service | 4002                       |
| Redis             | 6379                       |

---

## API Documentation

| Service         | Swagger UI                                  |
|-----------------|---------------------------------------------|
| Patient Service | http://localhost:4000/swagger-ui/index.html |
| Auth Service    | http://localhost:4005/swagger-ui/index.html |

---

## Testing

### Integration Tests
```bash
mvn test
```
Validates auth and patient workflows end-to-end through the API Gateway using RestAssured.

### CI/CD Pipelines

Three automated GitHub Actions workflows run on every push and pull request:

| Workflow               | Purpose                                       |
|------------------------|-----------------------------------------------|
| `maven.yml`            | Builds all services                           |
| `docker-build.yml`     | Builds Docker images                          |
| `integration-test.yml` | Runs full integration tests via Docker Compose|

---

## Load Testing (k6)

Tested using k6 with an extreme load scenario — 600 concurrent VUs over 8 minutes.

| Scenario      | VUs | RPS  | Error Rate | p95 Latency |
|---------------|-----|------|------------|-------------|
| Moderate load | 100 | ~95  | 0%         | <2s ✅      |
| Extreme load  | 600 | ~77  | 0%         | 15s*        |

*Extreme load saturated local machine RAM and PostgreSQL write I/O across 18,000+ iterations (19GB writes). Zero errors maintained even under full resource exhaustion. Latency under extreme load is a local hardware constraint, not a code issue.

---

## Observability

Metrics exposed via **Spring Boot Actuator + Micrometer**, scraped by Prometheus every 5 seconds.

Grafana dashboards visualize:
- API latency and request throughput
- Cache hit / miss ratio (`custom.redis.cache.miss`)
- Service health metrics

---

## Repository Structure
```
api-gateway/         JWT validation, Redis rate limiting
auth-service/        Authentication and JWT issuance
patient-service/     Core domain (REST, Kafka, Redis, metrics)
billing-service/     gRPC billing microservice
analytics-service/   Kafka consumer

integration-tests/   End-to-end RestAssured tests
performance-test/    k6 load test scripts
infrastructure/      AWS CDK infrastructure code
monitoring/          Prometheus configuration
```

---

## Future Improvements

- Kubernetes deployment manifests (Helm charts)
- Distributed tracing with OpenTelemetry
- Service discovery with Consul or Eureka

---

## Author

**Rinit Bhowmick** — Backend developer focused on distributed systems and scalable microservices.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-rinit--bhowmick-blue)](https://linkedin.com/in/rinit-bhowmick)
[![GitHub](https://img.shields.io/badge/GitHub-rinit18-black)](https://github.com/rinit18)